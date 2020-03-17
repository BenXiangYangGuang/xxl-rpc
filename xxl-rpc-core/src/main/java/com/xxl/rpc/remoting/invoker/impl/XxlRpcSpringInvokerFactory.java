package com.xxl.rpc.remoting.invoker.impl;

import com.xxl.rpc.registry.ServiceRegistry;
import com.xxl.rpc.remoting.invoker.XxlRpcInvokerFactory;
import com.xxl.rpc.remoting.invoker.annotation.XxlRpcReference;
import com.xxl.rpc.remoting.invoker.reference.XxlRpcReferenceBean;
import com.xxl.rpc.remoting.provider.XxlRpcProviderFactory;
import com.xxl.rpc.util.XxlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * xxl-rpc invoker factory, init service-registry and spring-bean by annotation (for spring)
 * spring 和 xxlRpcInvokerFactory 的结合，包装了xxlRpcInvokerFactory，包含xxlRpcInvokerFactory属性，并管理他;
 * 1.启动注册中心的client链接，并start，和服务的discover
 * 2.发现xxlRpcReference注解，并管理远程调用服务；远程调用服务会开启，client 会开启 netty server模式，服务于client；
 * @author xuxueli 2018-10-19
 */
public class XxlRpcSpringInvokerFactory extends InstantiationAwareBeanPostProcessorAdapter implements InitializingBean,DisposableBean, BeanFactoryAware {
    private Logger logger = LoggerFactory.getLogger(XxlRpcSpringInvokerFactory.class);

    // ---------------------- config ----------------------

    private Class<? extends ServiceRegistry> serviceRegistryClass;          // class.forname
    private Map<String, String> serviceRegistryParam;


    public void setServiceRegistryClass(Class<? extends ServiceRegistry> serviceRegistryClass) {
        this.serviceRegistryClass = serviceRegistryClass;
    }

    public void setServiceRegistryParam(Map<String, String> serviceRegistryParam) {
        this.serviceRegistryParam = serviceRegistryParam;
    }


    // ---------------------- util ----------------------

    private XxlRpcInvokerFactory xxlRpcInvokerFactory;
    //开启注册工厂
    //初始化 bean 的时候执行，可以针对某个具体的bean进行配置
    @Override
    public void afterPropertiesSet() throws Exception {
        // start invoker factory
        xxlRpcInvokerFactory = new XxlRpcInvokerFactory(serviceRegistryClass, serviceRegistryParam);
        xxlRpcInvokerFactory.start();
    }
    //一个bean 实例 被创建之后，所要执行的方法
    //一个bean 实例，被创建之后，查看他的属性字段，字段是否有XxlRpcReference注解；
    @Override
    public boolean postProcessAfterInstantiation(final Object bean, final String beanName) throws BeansException {

        // collection
        //封装这个API Controller中的，所有@XxlRpcReference注解 远程服务调用的属性字段；
        final Set<String> serviceKeyList = new HashSet<>();
        //解析controller 中的@XxlRpcReference注解
        // parse XxlRpcReferenceBean
        //doWithFields 为每一个bean Class的属性调用callback回调函数
        ReflectionUtils.doWithFields(bean.getClass(), new ReflectionUtils.FieldCallback() {
            @Override
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                //属性包含@XxlRpcReference
                if (field.isAnnotationPresent(XxlRpcReference.class)) {
                    // valid
                    Class iface = field.getType();
                    if (!iface.isInterface()) {
                        throw new XxlRpcException("xxl-rpc, reference(XxlRpcReference) must be interface.");
                    }

                    XxlRpcReference rpcReference = field.getAnnotation(XxlRpcReference.class);

                    // init reference bean
                    XxlRpcReferenceBean referenceBean = new XxlRpcReferenceBean(
                            rpcReference.netType(),
                            rpcReference.serializer().getSerializer(),
                            rpcReference.callType(),
                            rpcReference.loadBalance(),
                            iface,
                            rpcReference.version(),
                            rpcReference.timeout(),
                            rpcReference.address(),
                            rpcReference.accessToken(),
                            null,
                            xxlRpcInvokerFactory
                    );
                    //rpc 调用代理服务，从而像调用本地服务一样，调用远程服务；
                    Object serviceProxy = referenceBean.getObject();

                    // set bean
                    field.setAccessible(true);
                    //将serviceProxy这个值，绑定到bean实例的filed字段上;
                    field.set(bean, serviceProxy);

                    logger.info(">>>>>>>>>>> xxl-rpc, invoker factory init reference bean success. serviceKey = {}, bean.field = {}.{}",
                            XxlRpcProviderFactory.makeServiceKey(iface.getName(), rpcReference.version()), beanName, field.getName());

                    // collection
                    String serviceKey = XxlRpcProviderFactory.makeServiceKey(iface.getName(), rpcReference.version());
                    serviceKeyList.add(serviceKey);

                }
            }
        });

        // mult discovery
        // 客户端需要的provider rpc 服务，到注册中心中去，发现provider远程服务
        if (xxlRpcInvokerFactory.getServiceRegistry() != null) {
            try {
                xxlRpcInvokerFactory.getServiceRegistry().discovery(serviceKeyList);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        return super.postProcessAfterInstantiation(bean, beanName);
    }

    //bean is disposable method
    @Override
    public void destroy() throws Exception {

        // stop invoker factory
        xxlRpcInvokerFactory.stop();
    }

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
