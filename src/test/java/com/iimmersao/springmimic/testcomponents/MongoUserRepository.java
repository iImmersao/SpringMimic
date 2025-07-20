package com.iimmersao.springmimic.testcomponents;

import com.iimmersao.springmimic.annotations.Repository;
import com.iimmersao.springmimic.model.MongoUserEntity;

@Repository
public interface MongoUserRepository extends UserRepository<MongoUserEntity, String> {
}
