package com.iimmersao.springmimic.repository;

import java.util.List;
import java.util.Optional;

public interface CrudRepository<T, ID> {
    Optional<T> findById(ID id);
    List<T> findAll();
    void save(T entity);
    void deleteById(ID id);
}
