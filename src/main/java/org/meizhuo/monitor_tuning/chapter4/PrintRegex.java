package org.meizhuo.monitor_tuning.chapter4;
import com.sun.btrace.BTraceUtils;
import com.sun.btrace.annotations.BTrace;
import com.sun.btrace.annotations.OnMethod;
import com.sun.btrace.annotations.ProbeClassName;
import com.sun.btrace.annotations.ProbeMethodName;

@BTrace
public class PrintRegex {
	
	@OnMethod(
	        clazz="com.imooc.monitor_tuning.chapter4.Ch4Controller",
	        method="/.*/"  //这里表示拦截Ch4Controller里面的所有方法
	)
	public static void anyRead(@ProbeClassName String pcn, @ProbeMethodName String pmn) {
		BTraceUtils.println(pcn+","+pmn);
		BTraceUtils.println();
    }
}
