package com.iimmersao.springmimic.model;

public interface BaseUserEntity<ID> {
    ID getId();
    void setId(ID id);
    String getUsername();
    void setUsername(String username);
    String getEmail();
    void setEmail(String email);
}
