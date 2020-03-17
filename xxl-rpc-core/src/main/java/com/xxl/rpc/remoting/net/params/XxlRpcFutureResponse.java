package com.xxl.rpc.remoting.net.params;

import com.xxl.rpc.remoting.invoker.XxlRpcInvokerFactory;
import com.xxl.rpc.remoting.invoker.call.XxlRpcInvokeCallback;
import com.xxl.rpc.util.XxlRpcException;

import java.util.concurrent.*;

/**
 * call back future
 * future 是代表着一个一部计算结果;
 * 客户端用于请求的处理，从而封装了XxlRpcRequest和XxlRpcResponse
 *
 * @author xuxueli 2015-11-5 14:26:37
 */
public class XxlRpcFutureResponse implements Future<XxlRpcResponse> {

	private XxlRpcInvokerFactory invokerFactory;

	// net data
	private XxlRpcRequest request;
	private XxlRpcResponse response;

	// future lock
	private boolean done = false;

	private Object lock = new Object();

	// callback, can be null
	private XxlRpcInvokeCallback invokeCallback;


	public XxlRpcFutureResponse(final XxlRpcInvokerFactory invokerFactory, XxlRpcRequest request, XxlRpcInvokeCallback invokeCallback) {
		this.invokerFactory = invokerFactory;
		this.request = request;
		this.invokeCallback = invokeCallback;

		// set-InvokerFuture
		setInvokerFuture();
	}


	// ---------------------- response pool ----------------------

	public void setInvokerFuture(){
		this.invokerFactory.setInvokerFuture(request.getRequestId(), this);
	}
	public void removeInvokerFuture(){
		this.invokerFactory.removeInvokerFuture(request.getRequestId());
	}


	// ---------------------- get ----------------------

	public XxlRpcRequest getRequest() {
		return request;
	}
	public XxlRpcInvokeCallback getInvokeCallback() {
		return invokeCallback;
	}


	// ---------------------- for invoke back ----------------------
	//XXL-RPC采用NIO进行底层通讯，但是NIO是异步通讯模型，调用线程并不会阻塞获取调用结果，因此，XXL-RPC实现了在异步通讯模型上的同步调用，即“sync-over-async”，实现原理如下，可参考上
	/*
		synchronized是Java中的关键字，是一种同步锁。它修饰的对象有以下几种：
			1. 修饰一个代码块，被修饰的代码块称为同步语句块，其作用的范围是大括号{}括起来的代码，作用的对象是调用这个代码块的对象；
			2. 修饰一个方法，被修饰的方法称为同步方法，其作用的范围是整个方法，作用的对象是调用这个方法的对象；
			3. 修改一个静态的方法，其作用的范围是整个静态方法，作用的对象是这个类的所有对象；
			4. 修改一个类，其作用的范围是synchronized后面括号括起来的部分，作用主的对象是这个类的所有对象。
	*/
	//	当没有明确的对象作为锁，只是想让一段代码同步时，可以创建一个特殊的对象来充当锁：
	//	https://blog.csdn.net/luoweifu/article/details/46613015
	public void setResponse(XxlRpcResponse response) {
		this.response = response;
		// 有点疑惑锁住了 lock 对象，但是请求不会去，获取这个对象的
		synchronized (lock) {
			done = true;
			lock.notifyAll();
		}
	}


	// ---------------------- for invoke ----------------------

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		// TODO
		return false;
	}

	@Override
	public boolean isCancelled() {
		// TODO
		return false;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public XxlRpcResponse get() throws InterruptedException, ExecutionException {
		try {
			return get(-1, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			throw new XxlRpcException(e);
		}
	}
	//设置一个超时等待
	@Override
	public XxlRpcResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (!done) {
			synchronized (lock) {
				try {
					if (timeout < 0) {
						lock.wait();
					} else {
						long timeoutMillis = (TimeUnit.MILLISECONDS==unit)?timeout:TimeUnit.MILLISECONDS.convert(timeout , unit);
						lock.wait(timeoutMillis);
					}
				} catch (InterruptedException e) {
					throw e;
				}
			}
		}

		if (!done) {
			throw new XxlRpcException("xxl-rpc, request timeout at:"+ System.currentTimeMillis() +", request:" + request.toString());
		}
		return response;
	}


}
