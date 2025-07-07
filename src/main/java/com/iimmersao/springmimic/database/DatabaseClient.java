package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.annotations.Bean;
import com.iimmersao.springmimic.web.PageRequest;

import java.util.List;
import java.util.Optional;

@Bean
public interface DatabaseClient {

    /**
     * Saves the given entity (insert or update).
     */
    <T> void save( T entity);

    /**
     * Finds an entity by its ID.
     */
    <T> Optional<T> findById(Class<T> entityType, Object id);

    /**
     * Finds all records of a given type.
     */
    <T> List<T> findAll(Class<T> entityType);

    /**
     * Updates an entity by its ID.
     */
    <T> void updateById(T entity);

    /**
     * Deletes an entity by its ID.
     */
    <T> void deleteById(Class<T> entityType, Object id);

    /**
     * Deletes all entities of a given type.
     */
    <T> void deleteAll(Class<T> entityType);

    <T> List<T> findAll(Class<T> entityType, PageRequest pageRequest);

    boolean existsBy(Class<?> entityType, String fieldName, Object value);

    long countBy(Class<?> entityType, String fieldName, Object value);
}
