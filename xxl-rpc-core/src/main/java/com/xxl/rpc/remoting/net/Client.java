package com.xxl.rpc.remoting.net;

import com.xxl.rpc.remoting.invoker.reference.XxlRpcReferenceBean;
import com.xxl.rpc.remoting.net.params.XxlRpcRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * i client
 * 消费者通过客户端进行,发情请求的优化,比如客户端的线程池,变异步请求,为同步请求;
 * @author xuxueli 2015-11-24 22:18:10
 */
public abstract class Client {
	protected static final Logger logger = LoggerFactory.getLogger(Client.class);


	// ---------------------- init ----------------------

	protected volatile XxlRpcReferenceBean xxlRpcReferenceBean;

	public void init(XxlRpcReferenceBean xxlRpcReferenceBean) {
		this.xxlRpcReferenceBean = xxlRpcReferenceBean;
	}


    // ---------------------- send ----------------------

	/**
	 * async send, bind requestId and future-response
	 *
	 * @param address
	 * @param xxlRpcRequest
	 * @return
	 * @throws Exception
	 */
	public abstract void asyncSend(String address, XxlRpcRequest xxlRpcRequest) throws Exception;

}
