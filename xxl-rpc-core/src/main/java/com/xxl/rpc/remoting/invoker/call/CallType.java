package com.xxl.rpc.remoting.invoker.call;

/**
 * rpc call type
 *
 * @author xuxueli 2018-10-19
 */
public enum CallType {

    //
    SYNC,//同步;同一个线程，future，get 阻塞获取结果

    FUTURE,//另起一个future进行，异步结果获取

    CALLBACK, //注册一个回调函数，成功或失败进行不同的处理；起一个线程池，处理回调函数;

    ONEWAY;//一种方式，只是简单的发送请求，返回xxlRpcResponse的简单形式


    public static CallType match(String name, CallType defaultCallType){
        for (CallType item : CallType.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return defaultCallType;
    }

}
