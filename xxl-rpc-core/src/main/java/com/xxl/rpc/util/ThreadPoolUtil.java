package com.xxl.rpc.util;

import java.util.concurrent.*;

/**创建线程池工具类
 * @author xuxueli 2019-02-18
 */
public class ThreadPoolUtil {

    /**
     * make server thread pool
     *
     * @param serverType
     * @return
     */
    public static ThreadPoolExecutor makeServerThreadPool(final String serverType){
        ThreadPoolExecutor serverHandlerPool = new ThreadPoolExecutor(
                60,
                300,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(1000),
                //线程工厂，创建一个 包含 以severType类型为名称 的线程
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "xxl-rpc, "+serverType+"-serverHandlerPool-" + r.hashCode());
                    }
                },
                //队列满后，直接抛出异常
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        throw new XxlRpcException("xxl-rpc "+serverType+" Thread pool is EXHAUSTED!"); //adj. 筋疲力尽的，疲惫不堪的；耗尽的，枯竭的
                    }
                });		// default maxThreads 300, minThreads 60

        return serverHandlerPool;
    }

}
