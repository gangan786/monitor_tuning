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



####   获取对象的值

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

####  	正则表达式匹配类名和方法名

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



### 5-2 Tomcat—manager监控

+ 文档：apache-tomcat-8.0.50\webapps\docs

+ 步骤：

  1. conf/tomcat-users.xml添加用户

     tomcat-users.xml

     ~~~xml
     <?xml version='1.0' encoding='utf-8'?>
     <role rolename="tomcat"/>
       <role rolename="manager-status"/>
       <role rolename="manager-gui"/>
       <role rolename="admin-gui"/>
       <user username="tomcat" password="babaai" roles="tomcat,manager-status,manager-gui"/>
       <user username="Gangan" password="liudongdong" roles="manager-gui,tomcat,admin-gui"  />
     
     </tomcat-users>
     ~~~

  2. conf/Catalina/localhost/manager.xml配置允许远程连接

     manager.xml

     ~~~xml
     <?xml version="1.0" encoding="UTF-8"?>
     <Context privileged="true" antiResourceLocking="false"
              docBase="${catalina.home}/webapps/manager">
       <Valve className="org.apache.catalina.valves.RemoteAddrValve"
              allow="127\.0\.0\.1" />
     </Context>
     ~~~

  3. 重启tomcat

  4. 本地访问127.0.0.1:8080/manager，输入用户密码

  5. 这种监控手段还是比较简陋的

+ 注意这里的例子是用本地Tomcat，试过用远程主机，出现403 Access Denied 应该是网络的原因，查了资料没解决 这里贴一个[博客](http://blog.51cto.com/pizibaidu/2085954)比较详细







### 5-3 psi-prode监控

+ 这款Tomcat监控工具比Tomcat-manager更强大，配置过程为
  1. 首先下载：[GitHub](https://github.com/psi-probe/psi-probe)，我是直接下载zip
  2. 使用Maven对下载解压的工程进行打包：mav clean package -DMaven.test.skip
  3. 打包后的文件在`web/target/probe.war`
  4. 我是把上面对Tomcat-manage的两个配置文件配置到自己远程Tomcat上，并把打好的war包部署，启动Tomcat，成功访问。这个没有tomcat-manage远程不能访问的问题
+ 具体的使用方法还是看文档



### 5-4 Tomcat优化

+ 内存优化

  后期JVM内存知识点单独讲解

+ 线程优化

  + 介绍文档：webapps/docs/config/http.html，详细介绍推荐查看文档
    1. maxConnection：（即请求的连接数）由于使用了NIO，这里的最大的默认连接数可以达到
    2. acceptCount：当连接数达到最大连接数maxConnection时，会构建容量为acceptCount的缓存队列
    3. maxThreads：最大线程数，即同一时刻的并发线程数，这个参数取决于具体机器的CPU配置
    4. minSpaceThreads：最小空闲工作线程

+ 配置优化

  1. autoDeploy：是否开启线程周期检查有没有新应用被添加，生产环境禁止开启
  2. enableLookups：是否开启DNS查询，花费性能，生产环境禁止开启
  3. reloadable：开启的话会重新载入发生变化的类，默认关闭，生产环境禁止开启

+ Session优化

  如果不用session可以禁用，利用Redis









### 6 Nginx监控调优

只是粗略学习



### 7-1 JVM内存结构

+ 程序计数器 （PC Register）

  JVM支持多线程同时执行，每一个线程都有自己的PC Register，线程正在执行的方法叫做当前方法，如果是java代码，PC  Register里面存放的就是当前正在执行指令的地址，如果C代码，则为空。

+ 虚拟机栈 JVM Stacks

  Java虚拟机栈是线程私有的，他的生命周期与线程相同。虚拟机栈描述的是java方法执行的内存模型：每个方法在执行的同时会创建一个栈帧，用于存储当前方法的局部变量表，操作数栈，动态链接，方法出口等信息。每个方法从调用直至执行完成的过程，就对应着一个栈帧在虚拟机栈中入栈到出栈的过程。例如某个方法抛出异常了，打印的栈帧信息就是依赖上述模型。

+ 堆 Heap

  Java堆（Java Heap）是java虚拟机所管理的内存中最大的一块。堆是被所有线程共享的一块内存区域，在虚拟机启动时创建。此内存区域的唯一目的就是存放对象实例，几乎所有的对象实例都在这里分配内存。Java堆可以处于物理上不连续的内存空间中，只要是逻辑上连续的即可。

+ 方法区 Method Area

  方法区和Java堆一样，是各个线程共享的内存区域，他用于存储已被虚拟机加载的类信息，常量，静态变量，即时编译器编译后的代码等数据。虽然java虚拟机规范把方法区描述为堆的一个逻辑部分，但他却有一个别名：Non-Heap（非堆），目的是把java堆区分出来。

+ 常量池 Run-Time Constant Pool

  运行时常量池是方法区的一部分。Class文件中除了有类的版本，字段，方法，接口等描述信息外，还有一项信息就是常量池（Constant Pool Table），用于存放编译期生成的各种字面量和符号引用，这部分内容将在类加载后进入方法区的运行时常量池中存放。

+ 本地方法栈  Native Method Stacks

  本地方法栈（Native Method Stack）与虚拟机栈所发挥的作用是相似的，他们之间额区别不过是虚拟机栈为虚拟机执行Java方法（也就是字节码）服务，而本地方法栈则为虚拟机使用到的Native方法服务





 常用参数：

+ -Xms  -Xmx：最小堆内存，最大堆内存
+ -XX:NewSize   -XX:MaxNewSize：新生代大小，最大新生代大小
+ -XX:NewRatio：new区和old区的比例
+ -XX:SurvivorRatio：Eden区和SurvivorRatio区的比例
+ -XX:MetaspaceSize 
+ -XX:MaxMetaspaceSize
+ -XX:+UseCompressedClassPointers：是否启用类指针压缩或者说启用CCS
+ -XX:CompressedClassSpaceSize：启用类指针压缩后，指定存放该指针的CSS的大小
+ -XX:InitialCodeCacheSize：指定CodeCache的初始化大小
+ -XX:ReservedCodeCacheSize：指定CodeCache最大值







### 7-2 垃圾回收算法

思想：枚举根节点，做可达性分析

根节点：类加载器，Thread，虚拟机的本地变量表，static成员，常量引用，本地方法栈的变量等等



#### 常用垃圾回收算法：

+ 标记清除

  1. 原理：算法分为标记和清除两个阶段：首先标记出所有需要回收的对象，在标记完成后统一回收所有。

  2. 缺点：标记和清除两个过程效率都不高，产生碎片导致提前GC

+ 复制

  1. 原理：他将可用内存按容量划分为大小相等的两块，每次只使用其中的一块。当这一块的内存用完了，就将还活着的对象复制到另外一块上面，然后再把已使用过的内存空间一次性清理掉
  2. 优缺点：实现简单，运行高效，但空间利用率低

+ 标记整理

  1. 原理：标记过程任然与 "标记-清除" 算法一样，但后续步骤不是直接对可回收对象进行清理，而是让所有存活对象都向一端移动，然后直接清理掉端边界以外的内存
  2. 优缺点：没有了内存碎片，但整理内存比较耗时



#### JVM使用分代垃圾回收

+ Young区用复制算法
+ Old区用标记清除或者标记整理





#### 对象分配

+ 对象优先在Eden去分配
+ 大对象直接进入老年代：-XX:PretenureSizeThreshold
+ 长期存活对象进入老年代：-XX:MaxTenuringThreshold，-XX:+PrintTenuringDistribution，                                                -XX:TargetSurvivorRatio



### 7-3,4 垃圾收集器

#### 垃圾收集器的种类

+ 穿行收集器Serial：Serial，Serial Old

  1. 开启串行收集器：-XX:+UseSerialGC（young区）-XX:UseSerialOldGC（Old区，默认开启），在要求高响应的情况下，是不会开启Young区的串行收集器

+ 并行收集器Parallel：Parallel Scavenge，Parallel Old，吞吐量

  1. 吞吐量优先

  2. 开启方式：-XX:+UseParallelGC，-XX:+UseParallelOldGC

  3. Server模式下默认收集器

  4. -XX:ParallelGCThreads=<N>，指定并行垃圾回收线程的数量

  5. 自适应：Parallel Collector Ergonomics，他会依据如下设定参数自动调整堆的大小

     + -XX:MaxGCPauseMillis=<N>
     + -XX:GCTimeRatio=<N>
     + -Xmx<N>
     + 动态内存调整
       + -XX:YoungGenerationSizeIncrement=<Y>
       + -XX:TenuredGenerationSizeIncrement=<T>
       + -XX:AdaptiveSizeDecrementScaleFactor=<D>

  6. 我的 1 核 2 G 阿里云服务器是没开并行收集器的

     ~~~bash
     [root@izwz91vdyajvh2cr6jksfjz ~]# jinfo -flag UseParallelGC 4375
     -XX:-UseParallelGC
     [root@izwz91vdyajvh2cr6jksfjz ~]# jinfo -flag UseParallelGC 9913
     -XX:-UseParallelGC
     [root@izwz91vdyajvh2cr6jksfjz ~]# jinfo -flag UseParallelOldGC 9913
     -XX:-UseParallelOldGC
     
     ~~~

+ 并发收集器Concurrent：CMS，G1，停顿时间

  1. 响应时间优先
  2. CMS：-XX:+UseConcMarkSweepGC（并发的标记清除），Old区使用的垃圾收集方式
  3. -XX:+UseParNewGC（用于Young区并发）
  4. -XX:+UseG1GC（对G1开启并发收集）

  #### CMS Collector

  + 并发收集，低停顿，低延迟，作为老年代收集器
  + CMS垃圾收集过程：
    1. CMS initial mark：初始标记Root，STW（Stop The World）
    2. CMS concurrent mark：并发标记
    3. CMS concurrent preclean：并发预清理
    4. CMS remark：重新标记，STW
    5. CMS concurrent sweep：并发清除
    6. CMS concurrent reset：并发重置
  + CMS缺点：CPU敏感，会产生浮动垃圾，空间碎片
  + CMS相关参数：
    1. -XX:ConcGCThreads：并发的GC线程数
    2. -XX:+UseCMSCompactAtFullCollection：FullGC之后做压缩，减少空间碎片
    3. -XX:CMSFullGCsBeforeCompaction：多少次FullGC之后压缩一次
    4. -XX:CMSInitiatingOccupancyFracton：指定百分比，当Old区占比大于指定百分比的时候触发FullGC 
    5. -XX:+UseCMSInitiatingOccupancyOnly：是否动态调
    6. -XX:+CMSScavengeBeforeRemark：FullGC之前先做YoungGC
    7. -XX:+CMSClassUnloadingEnabled：启用回收Perm区
  + iCMS：适用于单核或者双核，在java8中已经被废除使用了

  #### G1 Collector

  + 简介

    The first focus of G1 is to provide a solution for users running applications that require larger heaps with limited GC latency.This means heap sizes of around 6G or larger,and a satble and predictable pause time below 0.5 seconds.

    适用新生代，老年代

  + Young，Old逻辑上存在，把堆切成若干小块作为基本单位（Region）

  + G1的几个概念：

    1. Region（内存分配的基本单位）
    2. SATB：Snapshot-At-The-Beginning，它是通过Root Tracing得到的，GC开始时候存活对象的快照
    3. RSet：记录了其他Region中的对象引用本Region中对象的关系，属于point-into结构（谁引用了我的对象）

  + Young GC的过程：

    1. 新对象进入Eden区
    2. 存活对象拷贝到Survivor区
    3. 存活时间达到年龄阈值时，对象晋升到Old区

  + MixedGC

    1. 不是FullGC，回收所有的Young和部分Old

    2. global concurrent marking过程：

       2-1 Initial marking phase：标记GC Root，STW

       2-2 Root region scanning phase：标记存活Region

       2-3 Concurrent marking phase：标记存活对象

       2-4 Remark phase：重新标记，STW

       2-5 Cleanup phase：部分STW

    3. MixedGC时机

       + InitiatingHeapOccupancyPercent：堆占有率达到这个数值则触发global concurrent marking，默认45%
       + G1HeapWasterPercent：在global concurrent marking结束之后，可以知道有多少空间要被回收，在每次YGC之后和再次发生MixedGC之前，会检查垃圾占比是否到达此参数，只有达到了，下次才会发生MixedGC

    4. MixedGC相关参数

       + G1MixedGCLiveThresholdPercent：Old区的region被回收时候的存活对象占比
       + G1MixedGCCountTarget：一次global concurrent marking之后，最多执行MixedGC的次数
       + G1OldCSetRegionThresholdPercent：一次MixedGC中能被选入CSet的最多old区的region数量

    5. 常用参数：

       + -XX:+UseG1GC：开启G1
       + -XX:G1HeapRegionSize=n，region的大小，1-32M，2048个
       + -XX:MaxGCPauseMillis：最大停顿时间

    6. 最佳实践

       + 年轻代大小：避免使用-Xmn，-XX:NewRatio等显式设置Young区大小，会覆盖暂停时间目标
       + 暂停时间目标：暂停时间不要太严苛，其吞吐量目标是90%的应用程序时间和10%的垃圾回收时间，太严苛会直接影响吞吐量

    7. 是否需要切换到G1

       + 50%以上的堆被存活对象占用
       + 对象分配和晋升的速度变化非常大
       + 垃圾回收时间特别长，超过了一秒

#### 并行 VS 并发

+ 并行（Parallel）：指多条垃圾收集线程并行工作，但此时用户线程仍然处于等待状态。适合科学计算，后台处理等弱交互场景
+ 并发（Concurrent）：指用户线程与垃圾收集线程同时执行（但不一定是并行的，可能会交替执行），垃圾收集线程在执行的时候不会停顿用户程序的运行。适合对响应时间有要求的场景，比如WEB。



#### 停顿时间 VS 吞吐量

+ 停顿时间：垃圾收集器做垃圾回收的时候中断应用进行垃圾回收的时间。可以通过：-XX:MaxGCPauseMillis
+ 吞吐量：花在垃圾收集的时间和花在应用时间的占比。可以通过：-XX:GCTimeRatio=<n>，垃圾收集时间占：1/1+n
+ 在理想的条件下评判一个垃圾回收器的好坏是：在最大吞吐量的情况下，停顿时间最短



#### 垃圾收集器搭配

+ 

![](https://github.com/gangan786/monitor_tuning/blob/master/img/GC.png?raw=true)

#### 如何选择垃圾收集器

具体查看官方文档

+ 优先调整堆的大小让服务器自己来选择
+ 如果内存小于100M，使用串行收集器
+ 如果是单核，并且没有停顿时间的要求，选择串行或者JVM自己选
+ 如果允许停顿时间超过一秒，选择并行或者JVM自己选
+ 如果响应时间比较重要，并且时间不能超过1秒，使用并发收集器

