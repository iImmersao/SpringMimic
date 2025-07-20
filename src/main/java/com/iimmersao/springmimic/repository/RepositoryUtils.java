package com.iimmersao.springmimic.repository;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class RepositoryUtils {

    public static Class<?> extractEntityClass(Class<?> repositoryInterface) {
        Class<?> crudRepoClass = CrudRepository.class;
        for (Type genericInterface : repositoryInterface.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType pt) {
                if (CrudRepository.class.isAssignableFrom(repositoryInterface)) {
                    Type entityType = pt.getActualTypeArguments()[0];
                    if (entityType instanceof Class<?> clazz) {
                        return clazz;
                    }
                }
            }
        }
        throw new IllegalArgumentException("Could not extract entity type from: " + repositoryInterface.getName());
    }
}
