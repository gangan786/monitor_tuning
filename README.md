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