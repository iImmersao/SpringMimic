package com.iimmersao.springmimic.repository;

import com.iimmersao.springmimic.database.DatabaseClient;

import java.lang.reflect.Proxy;

public class RepositoryProxyFactory {
    private final DatabaseClient databaseClient;

    public RepositoryProxyFactory(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @SuppressWarnings("unchecked")
    public <T> T createRepository(Class<T> repositoryInterface, Class<?> entityType) {
        return (T) Proxy.newProxyInstance(
                repositoryInterface.getClassLoader(),
                new Class<?>[]{repositoryInterface},
                new RepositoryInvocationHandler(databaseClient, entityType, repositoryInterface)
        );
    }
}
