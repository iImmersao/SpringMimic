package com.iimmersao.springmimic.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iimmersao.springmimic.annotations.Column;
import com.iimmersao.springmimic.annotations.GeneratedValue;
import com.iimmersao.springmimic.annotations.Id;

public class MongoUserEntity implements BaseUserEntity<String> {
    @Id
    @GeneratedValue
    @Column(name = "id")
    @JsonProperty("_id")
    private String id;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    public MongoUserEntity() {}

    public MongoUserEntity(String username, String email) {
        this.username = username;
        this.email = email;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return String.format("User{id=%s, username='%s', email='%s'}", id, username, email);
    }
}
