package com.iimmersao.springmimic.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iimmersao.springmimic.annotations.*;
import org.bson.types.ObjectId;

@Entity
@Table(name = "users")
public class TestMongoUser {

    @Id
    @GeneratedValue
    @Column(name = "id")
    @JsonProperty("_id")
    private ObjectId id;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    public TestMongoUser() {}

    public TestMongoUser(String username, String email) {
        this.username = username;
        this.email = email;
    }

    // Getters and setters
    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return String.format("MongoUser{id=%s, username='%s', email='%s'}", id, username, email);
    }
}
