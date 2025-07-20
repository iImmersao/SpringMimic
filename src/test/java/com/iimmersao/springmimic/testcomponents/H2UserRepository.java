package com.iimmersao.springmimic.testcomponents;

import com.iimmersao.springmimic.annotations.Repository;
import com.iimmersao.springmimic.model.H2UserEntity;

@Repository
public interface H2UserRepository extends UserRepository<H2UserEntity, Integer> {
}
