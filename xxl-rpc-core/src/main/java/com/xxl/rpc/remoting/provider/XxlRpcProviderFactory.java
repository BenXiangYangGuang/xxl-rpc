package com.xxl.rpc.remoting.provider;

import com.xxl.rpc.registry.ServiceRegistry;
import com.xxl.rpc.remoting.net.NetEnum;
import com.xxl.rpc.remoting.net.Server;
import com.xxl.rpc.remoting.net.params.BaseCallback;
import com.xxl.rpc.remoting.net.params.XxlRpcRequest;
import com.xxl.rpc.remoting.net.params.XxlRpcResponse;
import com.xxl.rpc.serialize.Serializer;
import com.xxl.rpc.util.IpUtil;
import com.xxl.rpc.util.NetUtil;
import com.xxl.rpc.util.ThrowableUtil;
import com.xxl.rpc.util.XxlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * provider
 * 远程provider服务，创建工厂
 * 一个jvm实例，一个XxlRpcProviderFactory工厂;
 * XxlRpcProviderFactory 提供了一个最简单的服务rpc服务注册工厂，里面提供了netty作为服务器，netty服务器开启多线程注册服务；XxlRpcProviderFactory 包装了server（netty服务），提供了管理netty服务器的方法；
 *
 * xxl-register-admin是注册中心项目，提供了register，move，discovery，monitor，等多种方法；
 * xxl-job-admin；是一个xxl-register-admin中注册功能的简版，
 * 只提供provider的register 和 remove服务；然后注册到xxl-job-admin中JobApiController中；
 * 所以xxl-rpc中的provider服务是注册到了xxl-job-admin中JobApiController中，然后写入了数据库
 *
 * 主要作用:
 * 		1.开启服务端,server start
 * 	    2.在开启服务端之前,注册服务到注册中心;
 * 	    3.关闭服务之前,从注册中心移除服务
 *
 * @author xuxueli 2015-10-31 22:54:27
 */
public class XxlRpcProviderFactory {
	private static final Logger logger = LoggerFactory.getLogger(XxlRpcProviderFactory.class);

	// ---------------------- config ----------------------

	private NetEnum netType;
	private Serializer serializer;

	private String ip;					// for registry
	private int port;					// default port
	private String accessToken;

	private Class<? extends ServiceRegistry> serviceRegistryClass; //注册中心，通信服务注册类
	private Map<String, String> serviceRegistryParam;  //服务提供者注册自己需要的参数


	public XxlRpcProviderFactory() {
	}
	public void initConfig(NetEnum netType,
						  Serializer serializer,
						  String ip,
						  int port,
						  String accessToken,
						   Class<? extends ServiceRegistry> serviceRegistryClass,
						  Map<String, String> serviceRegistryParam) {

		// init
		this.netType = netType;
		this.serializer = serializer;
		this.ip = ip;
		this.port = port;
		this.accessToken = accessToken;
		this.serviceRegistryClass = serviceRegistryClass;
		this.serviceRegistryParam = serviceRegistryParam;

		// valid
		if (this.netType==null) {
			throw new XxlRpcException("xxl-rpc provider netType missing.");
		}
		if (this.serializer==null) {
			throw new XxlRpcException("xxl-rpc provider serializer missing.");
		}
		//获得服务提供者部署机器的ip，这个是XxlProviderFactory类
		if (this.ip == null) {
			this.ip = IpUtil.getIp();
		}
		if (this.port <= 0) {
			this.port = 7080;
		}
		//端口是否占用
		if (NetUtil.isPortUsed(this.port)) {
			throw new XxlRpcException("xxl-rpc provider port["+ this.port +"] is used.");
		}
		if (this.serviceRegistryClass != null) {
			if (this.serviceRegistryParam == null) {
				throw new XxlRpcException("xxl-rpc provider serviceRegistryParam is missing.");
			}
		}

	}


	public Serializer getSerializer() {
		return serializer;
	}

	public int getPort() {
		return port;
	}


	// ---------------------- start / stop ----------------------

	private Server server;
	private ServiceRegistry serviceRegistry;
	private String serviceAddress;


	//开启netty服务器,处理注册数据
	public void start() throws Exception {
		// start server
		serviceAddress = IpUtil.getIpPort(this.ip, port);
		server = netType.serverClass.newInstance();
		//用来注册服务端服务接口到注册中心;定义好CallBack,在服务端启动之后server.start(),调用server实现类NettyServer的start(),执行start()里面的onStarted();来注册服务端的服务接口;
		//server.start();NettyServer.start(),然后onStarted();然后startedCallback.run();
		server.setStartedCallback(new BaseCallback() {		// serviceRegistry started
			@Override
			public void run() throws Exception {
				// start registry
				if (serviceRegistryClass != null) {
					serviceRegistry = serviceRegistryClass.newInstance();
					//开启服务注册
					serviceRegistry.start(serviceRegistryParam);
					//注册服务
					if (serviceData.size() > 0) {
						serviceRegistry.registry(serviceData.keySet(), serviceAddress);
					}
				}
			}
		});
		//用来移除服务端注册服务接口,从注册中心中;
		//服务端destroy(),调用stop();调用onStop();调用stopedCallback.run();
		server.setStopedCallback(new BaseCallback() {		// serviceRegistry stoped
			@Override
			public void run() {
				// stop registry
				if (serviceRegistry != null) {
					if (serviceData.size() > 0) {
						serviceRegistry.remove(serviceData.keySet(), serviceAddress);
					}
					serviceRegistry.stop();
					serviceRegistry = null;
				}
			}
		});
		server.start(this);
	}
	//通过XxlRpcProviderFactory，stop，调用server.stop();server.stop,调用onStop();onStop()调用server绑定stopedCallback();stopedCallback()调用serviceRegistry.remove,并停止注册服务；
	public void  stop() throws Exception {
		// stop server
		server.stop();
	}


	// ---------------------- server invoke ----------------------
	//服务接口的key,和对应bean的缓存
	/**
	 * init local rpc service map
	 */
	private Map<String, Object> serviceData = new HashMap<String, Object>();
	public Map<String, Object> getServiceData() {
		return serviceData;
	}

	/**
	 * make service key
	 *
	 * @param iface
	 * @param version
	 * @return
	 */
	public static String makeServiceKey(String iface, String version){
		String serviceKey = iface;
		if (version!=null && version.trim().length()>0) {
			serviceKey += "#".concat(version);
		}
		return serviceKey;
	}

	/**
	 * add service
	 *
	 * @param iface
	 * @param version
	 * @param serviceBean
	 */
	public void addService(String iface, String version, Object serviceBean){
		String serviceKey = makeServiceKey(iface, version);
		serviceData.put(serviceKey, serviceBean);

		logger.info(">>>>>>>>>>> xxl-rpc, provider factory add service success. serviceKey = {}, serviceBean = {}", serviceKey, serviceBean.getClass());
	}
	//在返回结果给客户端时调用;
	//xxlRpcProviderFactory.invokeService(xxlRpcRequest);
	/**
	 * invoke service
	 * 根据request,包含调用类,调用方法;通过反射进行,调用服务端方法,并返回XxlRpcResponse对象
	 *
	 * @param xxlRpcRequest
	 * @return
	 */
	public XxlRpcResponse invokeService(XxlRpcRequest xxlRpcRequest) {

		//  make response
		XxlRpcResponse xxlRpcResponse = new XxlRpcResponse();
		xxlRpcResponse.setRequestId(xxlRpcRequest.getRequestId());

		// match service bean
		String serviceKey = makeServiceKey(xxlRpcRequest.getClassName(), xxlRpcRequest.getVersion());
		Object serviceBean = serviceData.get(serviceKey);

		// valid
		if (serviceBean == null) {
			xxlRpcResponse.setErrorMsg("The serviceKey["+ serviceKey +"] not found.");
			return xxlRpcResponse;
		}

		if (System.currentTimeMillis() - xxlRpcRequest.getCreateMillisTime() > 3*60*1000) {
			xxlRpcResponse.setErrorMsg("The timestamp difference between admin and executor exceeds the limit.");
			return xxlRpcResponse;
		}
		if (accessToken!=null && accessToken.trim().length()>0 && !accessToken.trim().equals(xxlRpcRequest.getAccessToken())) {
			xxlRpcResponse.setErrorMsg("The access token[" + xxlRpcRequest.getAccessToken() + "] is wrong.");
			return xxlRpcResponse;
		}

		try {
			// invoke 反射调用,返回结果
			Class<?> serviceClass = serviceBean.getClass();
			String methodName = xxlRpcRequest.getMethodName();
			Class<?>[] parameterTypes = xxlRpcRequest.getParameterTypes();
			Object[] parameters = xxlRpcRequest.getParameters();

            Method method = serviceClass.getMethod(methodName, parameterTypes);
            method.setAccessible(true);
			Object result = method.invoke(serviceBean, parameters);

			/*FastClass serviceFastClass = FastClass.create(serviceClass);
			FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, parameterTypes);
			Object result = serviceFastMethod.invoke(serviceBean, parameters);*/

			xxlRpcResponse.setResult(result);
		} catch (Throwable t) {
			// catch error
			logger.error("xxl-rpc provider invokeService error.", t);
			xxlRpcResponse.setErrorMsg(ThrowableUtil.toString(t));
		}

		return xxlRpcResponse;
	}

}
