package com.iimmersao.springmimic.transaction.components;

import com.iimmersao.springmimic.annotations.Component;
import com.iimmersao.springmimic.annotations.Inject;

import java.lang.reflect.Proxy;

@Component
public class TransactionalConsumer {
    @Inject
    private TransactionalTestService transactionalTestService;

    public void createUserThenFail(String username) {
        transactionalTestService.createUserThenFail(username);
    }

    public boolean hasProxyService() {
        return Proxy.isProxyClass(transactionalTestService.getClass());
    }
}