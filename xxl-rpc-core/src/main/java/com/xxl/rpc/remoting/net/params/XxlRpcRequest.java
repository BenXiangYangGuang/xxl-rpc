package com.xxl.rpc.remoting.net.params;

import java.io.Serializable;
import java.util.Arrays;

/**
 * request
 * Rpc请求封装类
 * 每一个一个rpc请求是多样的，so抽象一层，来封装所有的Rpc请求
 * @author xuxueli 2015-10-29 19:39:12
 */
public class XxlRpcRequest implements Serializable{
	private static final long serialVersionUID = 42L;
	
	private String requestId;
	private long createMillisTime;
	private String accessToken;

    private String className;   //调用的接口
    private String methodName;  //调用的方法
    private Class<?>[] parameterTypes; //参数类型
    private Object[] parameters; //参数

	private String version; //版本


	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public long getCreateMillisTime() {
		return createMillisTime;
	}

	public void setCreateMillisTime(long createMillisTime) {
		this.createMillisTime = createMillisTime;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public Class<?>[] getParameterTypes() {
		return parameterTypes;
	}

	public void setParameterTypes(Class<?>[] parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	public Object[] getParameters() {
		return parameters;
	}

	public void setParameters(Object[] parameters) {
		this.parameters = parameters;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return "XxlRpcRequest{" +
				"requestId='" + requestId + '\'' +
				", createMillisTime=" + createMillisTime +
				", accessToken='" + accessToken + '\'' +
				", className='" + className + '\'' +
				", methodName='" + methodName + '\'' +
				", parameterTypes=" + Arrays.toString(parameterTypes) +
				", parameters=" + Arrays.toString(parameters) +
				", version='" + version + '\'' +
				'}';
	}

}
