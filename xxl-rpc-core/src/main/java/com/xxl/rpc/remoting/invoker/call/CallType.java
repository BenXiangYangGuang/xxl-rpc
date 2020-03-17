package com.xxl.rpc.remoting.invoker.call;

/**
 * rpc call type
 *
 * @author xuxueli 2018-10-19
 */
public enum CallType {

    //同步
    SYNC,

    FUTURE,

    CALLBACK, //回调;注册一个回调函数

    ONEWAY;


    public static CallType match(String name, CallType defaultCallType){
        for (CallType item : CallType.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return defaultCallType;
    }

}
