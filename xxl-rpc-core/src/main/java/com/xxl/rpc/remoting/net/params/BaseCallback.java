package com.xxl.rpc.remoting.net.params;

/**
 * @author xuxueli 2018-10-19
 * 一个基础的回调抽象类
 * server 的启动和停止，进行回调的执行方法的抽象类
 */
public abstract class BaseCallback {

    public abstract void run() throws Exception;

}
