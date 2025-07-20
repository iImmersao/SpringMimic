package com.iimmersao.springmimic.testcomponents;

import com.iimmersao.springmimic.model.BaseUserEntity;
import com.iimmersao.springmimic.repository.CrudRepository;
import com.iimmersao.springmimic.web.PageRequest;

import java.util.List;

public interface UserRepository<T extends BaseUserEntity<ID>, ID> extends CrudRepository<T, ID> {
    T findByUsername(String username);
    T findByEmail(String email);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    long countByUsername(String username);
    long countByEmail(String email);

    List<T> findByUsernameContains(String partialName);
    List<T> findByUsernameContains(String partialName, PageRequest pageRequest);

    List<T> findAll(PageRequest pageRequest);

    void deleteAll();
}
