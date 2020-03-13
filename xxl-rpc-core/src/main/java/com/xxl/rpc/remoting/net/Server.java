package com.xxl.rpc.remoting.net;

import com.xxl.rpc.remoting.net.params.BaseCallback;
import com.xxl.rpc.remoting.provider.XxlRpcProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * server
 * 服务器;服务器是什么呢？是一个由若干线程组成的提供执行方法的集合，能启动，关闭；
 *
 * @author xuxueli 2015-11-24 20:59:49
 */
public abstract class Server {
	protected static final Logger logger = LoggerFactory.getLogger(Server.class);

	//服务 启动之后回调函数，一个执行一些操作的线程抽象类
	private BaseCallback startedCallback;
	//服务 停止之后回调函数，一个执行一些操作的线程抽象类
	private BaseCallback stopedCallback;

	public void setStartedCallback(BaseCallback startedCallback) {
		this.startedCallback = startedCallback;
	}

	public void setStopedCallback(BaseCallback stopedCallback) {
		this.stopedCallback = stopedCallback;
	}


	/**
	 * start server
	 *
	 * @param xxlRpcProviderFactory
	 * @throws Exception
	 */
	public abstract void start(final XxlRpcProviderFactory xxlRpcProviderFactory) throws Exception;

	/**
	 * callback when started
	 */
	public void onStarted() {
		if (startedCallback != null) {
			try {
				startedCallback.run();
			} catch (Exception e) {
				logger.error(">>>>>>>>>>> xxl-rpc, server startedCallback error.", e);
			}
		}
	}

	/**
	 * stop server
	 *
	 * @throws Exception
	 */
	public abstract void stop() throws Exception;

	/**
	 * callback when stoped
	 */
	public void onStoped() {
		if (stopedCallback != null) {
			try {
				stopedCallback.run();
			} catch (Exception e) {
				logger.error(">>>>>>>>>>> xxl-rpc, server stopedCallback error.", e);
			}
		}
	}

}
