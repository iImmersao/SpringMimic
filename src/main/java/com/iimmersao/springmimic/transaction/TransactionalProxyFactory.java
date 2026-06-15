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
                && hasTransactionalAnnotation(bean.getClass());
    }

    public Object createProxy(Object bean) {
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