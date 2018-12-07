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





### 4-4 拦截复杂参数，环境变量，正则匹配拦截

+ this：@Self
+ 入参：可以用AnyType，也可以用真实类型，参考重载方法的拦截方式
+ 返回：@Return



####获取对象的值

+ 简单类型：直接获取

+ 复杂类型：反射，类名+属性名

  + 注意这里由于引入了User这个类，那么在编译脚本的时候就要引入该类所在的classpath（即User.class所在的路径：D:\Users\mr.gan\SpringBootProject\monitor_tuning\target\classes）

  + 如果没引入的话会编译报错

    ~~~powershell
    D:\Users\mr.gan\SpringBootProject\monitor_tuning\src\main\java\org\meizhuo\monitor_tuning\chapter4>btrace 19760 PrintArgComplex.java
    PrintArgComplex.java:12: 错误: 程序包org.meizhuo.monitor_tuning.chapter2不存在
    import org.meizhuo.monitor_tuning.chapter2.User;
                                              ^
    PrintArgComplex.java:23: 错误: 找不到符号
            public static void anyRead(@ProbeClassName String pcn, @ProbeMethodName String pmn, User user) {
                                                                                                ^
      符号:   类 User
      位置: 类 org.meizhuo.monitor_tuning.chapter4.PrintArgComplex
    BTrace compilation failed
    ~~~

  + 正确的使用命令

    ~~~powershell
    D:\Users\mr.gan\SpringBootProject\monitor_tuning\src\main\java\org\meizhuo\monitor_tuning\chapter4>btrace -cp "D:\Users\mr.gan\SpringBootProject\monitor_tuning\target\classes" 19760 PrintArgComplex.java
    {id=1, name=刘冬冬, }
    刘冬冬
    org.meizhuo.monitor_tuning.chapter4.Ch4Controller,arg2
    ~~~




+ 脚本示例


  ~~~java
@BTrace
public class PrintArgComplex {
	
	
	@OnMethod(
	        clazz="org.meizhuo.monitor_tuning.chapter4.Ch4Controller",
	        method="arg2",
	        location=@Location(Kind.ENTRY)
	)
	public static void anyRead(@ProbeClassName String pcn, @ProbeMethodName String pmn, User user) {
		//print all fields
		BTraceUtils.printFields(user);
		//print one field
		Field filed2 = BTraceUtils.field("org.meizhuo.monitor_tuning.chapter2.User", "name");
		BTraceUtils.println(BTraceUtils.get(filed2, user));
		BTraceUtils.println(pcn+","+pmn);
		BTraceUtils.println();
    }
}

  
  ~~~

#### 正则表达式匹配类名和方法名

~~~java
@BTrace
public class PrintRegex {
	
	@OnMethod(
	        clazz="com.imooc.monitor_tuning.chapter4.Ch4Controller",
	        method="/.*/"  	//这里表示拦截Ch4Controller里面的所有方法
	)
	public static void anyRead(@ProbeClassName String pcn, @ProbeMethodName String pmn) {
		BTraceUtils.println(pcn+","+pmn);
		BTraceUtils.println();
    }
}

~~~



#### 其他

+ 打印行号

+ 打印堆栈：Threads.jstack()

+ 打印环境变量

  ~~~java
  @BTrace
  public class PrintJinfo {
      static {
      	BTraceUtils.println("System Properties:");
      	BTraceUtils.printProperties();
      	BTraceUtils.println("VM Flags:");
      	BTraceUtils.printVmArguments();
      	BTraceUtils.println("OS Enviroment:");
      	BTraceUtils.printEnv();
      	BTraceUtils.exit(0);
      }
  }
  ~~~



### 4-5 注意事项

+ **默认只能本地运行**
+ 生产环境下可以使用，但是被修改的字节码不会被还原



### 5-1 Tomcat远程debug

+ 通信协议：JDWP（Java Debug Wire Protocol）：他定义了调试器（debugger）和被调试的java虚拟机（target VM）之间的通信协议，Tomcat支持此协议

+ 这里讲一下具体的配置吧

  + 首先我使用的是Tomcat8，VMware虚拟了CentOS7，Java8，Intellij IDEA

  + 首先修改Tomcat的配置文件 `tomcat/bin/startup.sh`开启jpda（添加`jpda`）

    ~~~shell
    exec "$PRGDIR"/"$EXECUTABLE" jpda start "$@"
    ~~~

  + 然后配置具体端口`tomcat/bin/catalina.sh` 找到如下内容并将`JPDA_ADDRESS` 的值改为对应端口，这个端口就是远程调试与本地通信的端口，如果你是用的类似阿里云的云服务器那么记得在安全组中放行此端口,我这里开放的端口是：54321，部分配置如下所示：

    ~~~shell
    if [ "$1" = "jpda" ] ; then
      if [ -z "$JPDA_TRANSPORT" ]; then
        JPDA_TRANSPORT="dt_socket"
      fi
      if [ -z "$JPDA_ADDRESS" ]; then
        JPDA_ADDRESS="54321"
      fi
      if [ -z "$JPDA_SUSPEND" ]; then
        JPDA_SUSPEND="n"
      fi
      if [ -z "$JPDA_OPTS" ]; then
        JPDA_OPTS="-agentlib:jdwp=transport=$JPDA_TRANSPORT,address=$JPDA_ADDRESS,server=y,suspend=$JPDA_SUSPEND"
      fi
      CATALINA_OPTS="$JPDA_OPTS $CATALINA_OPTS"
      shift
    fi
    
    ~~~

  + 查看服务端启动参数

    ~~~shell
    [root@localhost apache-tomcat-8.0.50]# ps -ef | grep tomcat
    root       9167      1  1 14:48 pts/0    00:00:30 /usr/bin/java -Djava.util.logging.config.file=/usr/tomcat/apache-tomcat-8.0.50/conf/logging.properties -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager -Djdk.tls.ephemeralDHKeySize=2048 -Djava.protocol.handler.pkgs=org.apache.catalina.webresources -agentlib:jdwp=transport=dt_socket,address=54321,server=y,suspend=n -Dignore.endorsed.dirs= -classpath /usr/tomcat/apache-tomcat-8.0.50/bin/bootstrap.jar:/usr/tomcat/apache-tomcat-8.0.50/bin/tomcat-juli.jar -Dcatalina.base=/usr/tomcat/apache-tomcat-8.0.50 -Dcatalina.home=/usr/tomcat/apache-tomcat-8.0.50 -Djava.io.tmpdir=/usr/tomcat/apache-tomcat-8.0.50/temp org.apache.catalina.startup.Bootstrap start
    ~~~

  + 那么此时服务端已经配置好了，接下来就是调试端了，我这里用的是Intellij IDEA，具体配置如图所示：

    ![avater](https://github.com/gangan786/monitor_tuning/blob/master/img/remoteDebug.png?raw=true)

  + 那么此时IDEA以debug模式运行的时候就可以和往常那样Debug了

+ 其实远程Debug是JVM支持的，所以只要在启动进程的时候添加如下运行参数即可实现远程debug的功能

  ~~~shell
  -agentlib:jdwp=transport=dt_socket,address=54321,server=y,suspend=n
  ~~~


