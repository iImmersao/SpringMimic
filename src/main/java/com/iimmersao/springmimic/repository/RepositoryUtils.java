package com.iimmersao.springmimic.repository;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class RepositoryUtils {

    public static Class<?> extractEntityClass(Class<?> repositoryInterface) {
        for (Type genericInterface : repositoryInterface.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType pt &&
                    pt.getRawType().getTypeName().equals(CrudRepository.class.getName())) {

                Type entityType = pt.getActualTypeArguments()[0];
                if (entityType instanceof Class<?> clazz) {
                    return clazz;
                }
            }
        }
        throw new IllegalArgumentException("Could not extract entity type from: " + repositoryInterface.getName());
    }
}
