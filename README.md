# Monitor_Tuning

用于java性能监控与调优的学习记录

教程来自慕课网实战教程：[Java生产环境下性能监控与调优详解](https://coding.imooc.com/class/241.html)

java命令行[文档](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/)

运行环境：

+ jdk1.8
+ SpringBoot



### 2-1 JVM的参数类型

+ 标准参数

  比较稳定

  + -help
  + -service -client
  + -version -showversion
  + -cp -classpath

+ X参数

  非标准参数

  + -Xint：解释执行
  + -Xcomp：第一次使用就编译成本地代码
  + -Xmixed：混合模式，JVM自己来决定是否编译成本地代码

+ XX参数

  非标准转化函数，相对不稳定，主要用于JVM调优和Debug

  + Boolean类型

    格式：-XX:[+/-]<name> 表示启用或者禁止name属性（‘+’：表示启用，’-‘：表示禁用）

  + 非Boolean类型

    格式：-XX:<name>=<value>表示name属性的值是value

    EX：

    ​	-Xms等价于-XX:InitialHeapSize

    ​	-Xmx等价于-XX:MaxHeapSize





### 2-2 查看JVM运行时参数

+ -XX:+PrintFlagsInitial：查看初始化参数
+ -XX:+PrintFlagsFinal：查看运行时参数
+ jps：查看java进程
+ jinfo：查看对应进程的参数，如：jinfo -flag MaxHeapSize pid



### 2-3 查看JVM统计信息

+ [jstate](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/jstat.html#BEHHGFAE)
  + 查看类加载的信息：jstate -class
  + 查看垃圾回收的信息：jstate -gc
  + 查看 JIT 编译效果：jstate -compiler  





### 2-4 演示内存溢出

演示内存溢出

了解下Metaspace



### 2-5 导出内存映像文件

+ 内存溢出自动导出

  -XX:+HeapDumpOnOutOfMemoryError：开启内存溢出时自动导出映像文件功能

  -XX:HeapDumpPath=./   ：导出路径

+ 使用jmap命令手动导出

  内存较大的时候会导出失败



### 2-6 MAT分析内存溢出

MAT的使用



### 2-7 jstate与线程状态



### 2-8 实战死循环导致CPU飙高

jstate定位死锁



### 3-1，3-2基于JVisualVM的可视化操作

+ 监控本地java进程
+ 监控远程进程



以下给出一个远程监控Tomcat的例子

要远程监控Tomcat首先修改该Tomcat的配置文件 `tomcat/bin/catalina.sh`

~~~scheme
JAVA_OPTS="$JAVA_OPTS -Djava.rmi.server.hostname=120.79.78.179 -Dcom.sun.management.jmxremote.port=19000 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
~~~

参数解释（具体步骤Google）：

+ hostname：主机ip
+ port：监控端口
+ ssl：是否开启ssl验证
+ autnenticate：是否开启用户验证，如果开启的话要添加用户名和密码



**其实远程监控的配置其实就是要在进程启动的时候，告诉JVM你的具体配置（如上），Tomcat在catalina.sh配置的目的就是告诉JVM具体配置，对于其他应用也是，只要启动的时候将具体的配置告诉JVM就行**

这里我遇到了一个问题：由于我的服务器是阿里云服务器，即使配置正确，安全组放行，也还是本地连不上云端服务器，阿里云我用的是Tomcat8，后来我只好用本地的虚拟机开了一个Linux服务器，使用的是Tomcat7，然后还是按照原来的配置就连接成功，可以监控了

### 4-1 基于Btrace的监控调试

#### 简介

+ BTrace可以动态的向目标应用程序的字节码注入追踪代码
+ JavaComplierApi，JVMTI，Agent，Instrumentation+ASM
+ BTract的具体使用：[GitHub](https://github.com/btraceio/btrace)

#### BTrace的安装

1. 首先下载对应平台的[BTrace](https://github.com/btraceio/btrace/releases/tag/v1.3.11.2)
2. 配置环境变量
   + 变量名：BTRACE_HOME
   + 变量值：D:\Application\btrace-bin-1.3.11.2（所下载的Btrace的绝对路径）
   + 然后将BTRACE_HOME添加进PATH环境变量中
     + %BTRACE_HOME%\bin
3. 编写脚本的时候要导入的包在btrace-bin-1.3.11.2/build里面

#### 两种运行脚本方式

+ 在JVisualVM中添加BTrace插件，添加classpath
+ 使用命令行btrace <pid> <trace_script>
  + pid：监控进程ID
  + trace_script：脚本的名称
  + 例如：`D:\Users\mr.gan\SpringBootProject\monitor_tuning\src\main\java\org\meizhuo\monitor_tuning\chapter4>btrace 16960 PrintArgSimple.java` 注意：这个命令要到脚本的绝对路径下进行执行

#### 脚本示例

~~~java
@BTrace
public class PrintArgSimple {
	
	@OnMethod(
	        clazz="org.meizhuo.monitor_tuning.chapter4.Ch4Controller",//指定监控的类名
	        method="arg1",//指定监控类中的方法名
	        location=@Location(Kind.ENTRY)//拦截的时机，这里是方法执行前
	)
	/**
	 * 方法名任意
	 */
	public static void anyRead(@ProbeClassName String pcn, @ProbeMethodName String pmn, AnyType[] args) {
		BTraceUtils.printArray(args);//方法参数
		BTraceUtils.println(pcn+","+pmn);//类名+方法名
		BTraceUtils.println();
    }
}
~~~

虽然在 Java VisualVM上也可以编辑脚本代码，但是很不方便，建议用IDE写好粘贴过去

#### 关于BTrace工作方式的思考

+ Btrace将对应的脚本文件编译成字节码文件嵌入到要监控的代码块中，如果没有JAVA VisualVM连接到远程的进程上，那么只能在远程主机编译脚本并运行，这样势必会重新安装对应平台的Btrace，繁琐。如果可以远程连接上就可以在本地编译完成后，交由Java VisualVM传送到远程机器上，这样会方便很多

 

### 4-2 拦构造函数，重载函数

+ 拦截方法

  + 拦截构造函数

    ~~~java
    @BTrace
    public class PrintConstructor {
    	
    	@OnMethod(
    	        clazz="com.imooc.monitor_tuning.chapter2.User",
    	        method="<init>"
    	)
    	public static void anyRead(@ProbeClassName String pcn, @ProbeMethodName String pmn, AnyType[] args) {
    		BTraceUtils.println(pcn+","+pmn);
    		BTraceUtils.printArray(args);
    		BTraceUtils.println();
        }
    }
    ~~~

  + 拦截同名函数用参数类型进行区分

    ~~~java
    @BTrace
    public class PrintSame {
    	
    	@OnMethod(
    	        clazz="com.imooc.monitor_tuning.chapter4.Ch4Controller",
    	        method="same"
    	)
    
        /**
         * 此方法的后面的参数排列代表拦截函数的参数排列
         */
    	public static void anyRead(@ProbeClassName String pcn, @ProbeMethodName String pmn, String name) {
    		BTraceUtils.println(pcn+","+pmn + "," + name);
    		BTraceUtils.println();
        }
    }
    
    ~~~





### 4-3 拦截时机



+ Kind.ENTRY：入口，默认值

+ Kind.RETURN：返回

  ~~~java
  @BTrace
  public class PrintReturn {
  	
  	@OnMethod(
  	        clazz="com.imooc.monitor_tuning.chapter4.Ch4Controller",
  	        method="arg1",
  	        location=@Location(Kind.RETURN)
  	)
  	public static void anyRead(@ProbeClassName String pcn, @ProbeMethodName String pmn, @Return AnyType result) {
  		BTraceUtils.println(pcn+","+pmn + "," + result);
  		BTraceUtils.println();
      }
  }
  
  ~~~

+ Kind.THROW：异常

  这个功能比较强大，使用的是官方推荐的脚本，只要被监测方法发生异常都可以被检测到

  ~~~java
  @BTrace 
  public class PrintOnThrow {    
      // store current exception in a thread local
      // variable (@TLS annotation). Note that we can't
      // store it in a global variable!
      @TLS 
      static Throwable currentException;
  
      // introduce probe into every constructor of java.lang.Throwable
      // class and store "this" in the thread local variable.
      @OnMethod(
          clazz="java.lang.Throwable",
          method="<init>"
      )
      public static void onthrow(@Self Throwable self) {//new Throwable()
          currentException = self;
      }
  
      @OnMethod(
          clazz="java.lang.Throwable",
          method="<init>"
      )
      public static void onthrow1(@Self Throwable self, String s) {//new Throwable(String msg)
          currentException = self;
      }
  
      @OnMethod(
          clazz="java.lang.Throwable",
          method="<init>"
      )
      public static void onthrow1(@Self Throwable self, String s, Throwable cause) {//new Throwable(String msg, Throwable cause)
          currentException = self;
      }
  
      @OnMethod(
          clazz="java.lang.Throwable",
          method="<init>"
      )
      public static void onthrow2(@Self Throwable self, Throwable cause) {//new Throwable(Throwable cause)
          currentException = self;
      }
  
      // when any constructor of java.lang.Throwable returns
      // print the currentException's stack trace.
      @OnMethod(
          clazz="java.lang.Throwable",
          method="<init>",
          location=@Location(Kind.RETURN)
      )
      public static void onthrowreturn() {
          if (currentException != null) {
          	BTraceUtils.Threads.jstack(currentException);
          	BTraceUtils.println("=====================");
              currentException = null;
          }
      }
  }
  
  ~~~

+ Kind.Line：行（验证代码是否执行到某行）

  ~~~java
  @BTrace
  public class PrintLine {
  	
  	@OnMethod(
  	        clazz="com.imooc.monitor_tuning.chapter4.Ch4Controller",
  	        method="exception",
  	        location=@Location(value=Kind.LINE, line=34)//如果代码执行到这行触发下面方法
  	)
  	public static void anyRead(@ProbeClassName String pcn, @ProbeMethodName String pmn, int line) {
  		BTraceUtils.println(pcn+","+pmn + "," +line);//行号
  		BTraceUtils.println();
      }
  }
  
  ~~~
