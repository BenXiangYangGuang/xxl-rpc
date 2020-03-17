package com.xxl.rpc.remoting.net.common;

import com.xxl.rpc.remoting.invoker.XxlRpcInvokerFactory;
import com.xxl.rpc.remoting.invoker.reference.XxlRpcReferenceBean;
import com.xxl.rpc.remoting.net.params.BaseCallback;
import com.xxl.rpc.remoting.net.params.XxlRpcRequest;
import com.xxl.rpc.serialize.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 客户端链接抽象类
 * @author xuxueli 2018-10-19
 */
public abstract class ConnectClient {
    protected static transient Logger logger = LoggerFactory.getLogger(ConnectClient.class);


    // ---------------------- iface ----------------------

    public abstract void init(String address, final Serializer serializer, final XxlRpcInvokerFactory xxlRpcInvokerFactory) throws Exception;

    public abstract void close();

    public abstract boolean isValidate();
    //sync 同步发送
    public abstract void send(XxlRpcRequest xxlRpcRequest) throws Exception ;


    // ---------------------- client pool map ----------------------

    /**
     * async send
     */
    public static void asyncSend(XxlRpcRequest xxlRpcRequest, String address,
                                 Class<? extends ConnectClient> connectClientImpl,
                                 final XxlRpcReferenceBean xxlRpcReferenceBean) throws Exception {

        // client pool	[tips03 : may save 35ms/100invoke if move it to constructor, but it is necessary. cause by ConcurrentHashMap.get]
        ConnectClient clientPool = ConnectClient.getPool(address, connectClientImpl, xxlRpcReferenceBean);

        try {
            // do invoke
            clientPool.send(xxlRpcRequest);
        } catch (Exception e) {
            throw e;
        }

    }

    private static volatile ConcurrentMap<String, ConnectClient> connectClientMap;        // (static) alread addStopCallBack
    //客户端链接 在connectClientMap中进行更新，防止多线程更新，根据address进行 加锁
    private static volatile ConcurrentMap<String, Object> connectClientLockMap = new ConcurrentHashMap<>();


    //同步初始化ConnectClient容器 connectClientMap；并从中获取一个ConnectClient
    private static ConnectClient getPool(String address, Class<? extends ConnectClient> connectClientImpl,
                                         final XxlRpcReferenceBean xxlRpcReferenceBean) throws Exception {

        // init base compont, avoid repeat init
        if (connectClientMap == null) {
            synchronized (ConnectClient.class) {
                if (connectClientMap == null) {
                    // init
                    connectClientMap = new ConcurrentHashMap<String, ConnectClient>();
                    // stop callback 清除客户端
                    xxlRpcReferenceBean.getInvokerFactory().addStopCallBack(new BaseCallback() {
                        @Override
                        public void run() throws Exception {
                            if (connectClientMap.size() > 0) {
                                for (String key: connectClientMap.keySet()) {
                                    //key 是一个 client address 链接的地址；
                                    //一个address代表一个链接
                                    ConnectClient clientPool = connectClientMap.get(key);
                                    clientPool.close();
                                }
                                connectClientMap.clear();
                            }
                        }
                    });
                }
            }
        }

        // get-valid client
        ConnectClient connectClient = connectClientMap.get(address);
        if (connectClient!=null && connectClient.isValidate()) {
            return connectClient;
        }

        // lock
        Object clientLock = connectClientLockMap.get(address);
        if (clientLock == null) {
            connectClientLockMap.putIfAbsent(address, new Object());
            clientLock = connectClientLockMap.get(address);
        }

        // remove-create new client
        synchronized (clientLock) {

            // get-valid client, avlid repeat
            connectClient = connectClientMap.get(address);
            if (connectClient!=null && connectClient.isValidate()) {
                return connectClient;
            }

            // remove old
            if (connectClient != null) {
                connectClient.close();
                connectClientMap.remove(address);
            }

            // set pool
            ConnectClient connectClient_new = connectClientImpl.newInstance();
            try {
                //客户端链接初始化
                connectClient_new.init(address, xxlRpcReferenceBean.getSerializer(), xxlRpcReferenceBean.getInvokerFactory());
                connectClientMap.put(address, connectClient_new);
            } catch (Exception e) {
                connectClient_new.close();
                throw e;
            }

            return connectClient_new;
        }

    }

}
