package com.xxl.rpc.remoting.provider.impl;

import com.xxl.rpc.registry.ServiceRegistry;
import com.xxl.rpc.remoting.net.NetEnum;
import com.xxl.rpc.remoting.provider.XxlRpcProviderFactory;
import com.xxl.rpc.remoting.provider.annotation.XxlRpcService;
import com.xxl.rpc.serialize.Serializer;
import com.xxl.rpc.util.XxlRpcException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * xxl-rpc provider (for spring)
 * XxlRpcSpringProviderFactory 将 XxlRpcProviderFactory 功能和spring项目结合起来，进行XxlRpcProviderFactory初始化、启动
 * @author xuxueli 2018-10-18 18:09:20
 */
public class XxlRpcSpringProviderFactory extends XxlRpcProviderFactory implements ApplicationContextAware, InitializingBean,DisposableBean {

    // ---------------------- config ----------------------

    private String netType = NetEnum.NETTY.name();
    private String serialize = Serializer.SerializeEnum.HESSIAN.name();

    private String ip;          	// for registry;当前provider服务部署的所在机器ip；
    private int port;				// default port
    private String accessToken;

    private Class<? extends ServiceRegistry> serviceRegistryClass;                          // class.forname
    private Map<String, String> serviceRegistryParam;


    // set
    public void setNetType(String netType) {
        this.netType = netType;
    }

    public void setSerialize(String serialize) {
        this.serialize = serialize;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setServiceRegistryClass(Class<? extends ServiceRegistry> serviceRegistryClass) {
        this.serviceRegistryClass = serviceRegistryClass;
    }

    public void setServiceRegistryParam(Map<String, String> serviceRegistryParam) {
        this.serviceRegistryParam = serviceRegistryParam;
    }


    // util
    private void prepareConfig(){

        // prepare config
        NetEnum netTypeEnum = NetEnum.autoMatch(netType, null);
        Serializer.SerializeEnum serializeEnum = Serializer.SerializeEnum.match(serialize, null);
        Serializer serializer = serializeEnum!=null?serializeEnum.getSerializer():null;

        // init config
        super.initConfig(netTypeEnum, serializer, ip, port, accessToken, serviceRegistryClass, serviceRegistryParam);
    }


    // ---------------------- util ----------------------
    //发现XxlRpcService服务，并添加到服务列表集合
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {//setApplicationContext spring起来之后就执行的方法
        //获取标有XxlRpcService注解的服务类;首先这个服务类，必须标是spring管理的bean
        Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(XxlRpcService.class);
        if (serviceBeanMap!=null && serviceBeanMap.size()>0) {
            for (Object serviceBean : serviceBeanMap.values()) {
                // valid;必须继承api服务定义的接口方法
                if (serviceBean.getClass().getInterfaces().length ==0) {
                    throw new XxlRpcException("xxl-rpc, service(XxlRpcService) must inherit interface.");
                }
                // add service
                XxlRpcService xxlRpcService = serviceBean.getClass().getAnnotation(XxlRpcService.class);
                //获取接口的名称
                String iface = serviceBean.getClass().getInterfaces()[0].getName();
                //获取版本号
                String version = xxlRpcService.version();

                //发现服务端提供服务接口,进行服务端接口注册;
                super.addService(iface, version, serviceBean);
            }
        }

        // TODO，addServices by api + prop

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.prepareConfig(); //初始化服务参数
        super.start(); //开启XxlRpcProviderFactory服务，开始netty service服务
    }

    @Override
    public void destroy() throws Exception {
        //销毁XxlRpcProviderFactory服务，销毁netty service服务
        super.stop();
    }

}
