package com.xxl.rpc.remoting.invoker.generic;

/**
 * 泛化调用
 * XXL-RPC 提供 “泛华调用” 支持，服务调用方不依赖服务方提供的API；泛化调用通常用于框架集成，比如 “网关平台、跨语言调用、测试平台” 等；
 * 开启 “泛华调用” 时服务方不需要做任何调整，仅需要调用方初始化一个泛华调用服务Reference （”XxlRpcGenericService”） 即可。
 * @author xuxueli 2018-12-04
 */
public interface XxlRpcGenericService {

    /**
     * generic invoke
     *
     * @param iface                 iface name
     * @param version               iface version
     * @param method                method name
     * @param parameterTypes        parameter types, limit base type like "int、java.lang.Integer、java.util.List、java.util.Map ..."
     * @param args
     * @return
     */
    public Object invoke(String iface, String version, String method, String[] parameterTypes, Object[] args);

}