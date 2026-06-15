package com.iimmersao.springmimic.transaction;

import com.iimmersao.springmimic.annotations.Transactional;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TransactionalInvocationHandler implements InvocationHandler {
    private final Object target;
    private final TransactionManager transactionManager;

    public TransactionalInvocationHandler(Object target, TransactionManager transactionManager) {
        this.target = target;
        this.transactionManager = transactionManager;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (isObjectMethod(method)) {
            return method.invoke(target, args);
        }

        Method targetMethod = findTargetMethod(method);
        Transactional transactional = findTransactionalAnnotation(method, targetMethod);
        if (transactional == null) {
            return invokeTarget(targetMethod, args);
        }

        TransactionStatus status = transactionManager.begin(toDefinition(transactional));
        Object result;
        try {
            result = invokeTarget(targetMethod, args);
        } catch (Throwable throwable) {
            if (shouldRollback(throwable, transactional)) {
                rollback(status, throwable);
            } else {
                commit(status, throwable);
            }
            throw throwable;
        }

        transactionManager.commit(status);
        return result;
    }

    private Object invokeTarget(Method targetMethod, Object[] args) throws Throwable {
        try {
            targetMethod.setAccessible(true);
            return targetMethod.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private void rollback(TransactionStatus status, Throwable original) throws Throwable {
        try {
            transactionManager.rollback(status);
        } catch (Throwable rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    private void commit(TransactionStatus status, Throwable original) throws Throwable {
        try {
            transactionManager.commit(status);
        } catch (Throwable commitFailure) {
            original.addSuppressed(commitFailure);
        }
    }

    private TransactionDefinition toDefinition(Transactional transactional) {
        return new TransactionDefinition(
                transactional.propagation(),
                transactional.isolation(),
                transactional.readOnly(),
                transactional.timeoutSeconds()
        );
    }

    private boolean shouldRollback(Throwable throwable, Transactional transactional) {
        if (matches(throwable, transactional.noRollbackFor())) {
            return false;
        }
        if (matches(throwable, transactional.rollbackFor())) {
            return true;
        }
        return throwable instanceof RuntimeException || throwable instanceof Error;
    }

    private boolean matches(Throwable throwable, Class<? extends Throwable>[] exceptionTypes) {
        for (Class<? extends Throwable> exceptionType : exceptionTypes) {
            if (exceptionType.isAssignableFrom(throwable.getClass())) {
                return true;
            }
        }
        return false;
    }

    private Transactional findTransactionalAnnotation(Method interfaceMethod, Method targetMethod) {
        Transactional methodAnnotation = targetMethod.getAnnotation(Transactional.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        methodAnnotation = interfaceMethod.getAnnotation(Transactional.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        Transactional classAnnotation = target.getClass().getAnnotation(Transactional.class);
        if (classAnnotation != null) {
            return classAnnotation;
        }

        return interfaceMethod.getDeclaringClass().getAnnotation(Transactional.class);
    }

    private Method findTargetMethod(Method interfaceMethod) {
        try {
            return target.getClass().getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
        } catch (NoSuchMethodException e) {
            throw new TransactionException("Failed to find target method for transactional invocation: " + interfaceMethod, e);
        }
    }

    private boolean isObjectMethod(Method method) {
        return method.getDeclaringClass() == Object.class;
    }
}