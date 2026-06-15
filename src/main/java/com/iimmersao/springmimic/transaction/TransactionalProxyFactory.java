package com.iimmersao.springmimic.transaction;

import com.iimmersao.springmimic.annotations.Transactional;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class TransactionalProxyFactory {
    private final TransactionManager transactionManager;

    public TransactionalProxyFactory(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public boolean canCreateProxy(Object bean) {
        return bean != null
                && bean.getClass().getInterfaces().length > 0
                && isTransactionalBean(bean);
    }

    public boolean isTransactionalBean(Object bean) {
        return bean != null && hasTransactionalAnnotation(bean.getClass());
    }

    public String getProxySkipReason(Object bean) {
        if (!isTransactionalBean(bean)) {
            return null;
        }
        if (bean.getClass().getInterfaces().length == 0) {
            return "@Transactional bean " + bean.getClass().getName()
                    + " will not be proxied because it does not implement an interface. "
                    + "The current MVP supports interface-based transactional proxies only.";
        }
        if (transactionManager == null) {
            return "@Transactional bean " + bean.getClass().getName()
                    + " will not be proxied because no TransactionManager is registered.";
        }
        return null;
    }

    public Object createProxy(Object bean) {
        if (transactionManager == null) {
            throw new TransactionException("Cannot create transactional proxy without a TransactionManager");
        }
        Class<?>[] interfaces = bean.getClass().getInterfaces();
        return Proxy.newProxyInstance(
                bean.getClass().getClassLoader(),
                interfaces,
                new TransactionalInvocationHandler(bean, transactionManager)
        );
    }

    private boolean hasTransactionalAnnotation(Class<?> beanClass) {
        if (beanClass.isAnnotationPresent(Transactional.class)) {
            return true;
        }

        for (Method method : beanClass.getMethods()) {
            if (method.isAnnotationPresent(Transactional.class)) {
                return true;
            }
        }

        for (Class<?> iface : beanClass.getInterfaces()) {
            if (iface.isAnnotationPresent(Transactional.class)) {
                return true;
            }
            for (Method method : iface.getMethods()) {
                if (method.isAnnotationPresent(Transactional.class)) {
                    return true;
                }
            }
        }

        return false;
    }
}