package com.iimmersao.springmimic.model;

@SuppressWarnings(value = "unused")
public interface BaseUserEntity<ID> {
    ID getId();
    void setId(ID id);
    String getUsername();
    void setUsername(String username);
    String getEmail();
    void setEmail(String email);
}
