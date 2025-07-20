package com.iimmersao.springmimic.testcomponents;

import com.iimmersao.springmimic.annotations.Repository;
import com.iimmersao.springmimic.model.MySqlUserEntity;

@Repository
public interface MySqlUserRepository extends UserRepository<MySqlUserEntity, Integer> {
}
