package com.iimmersao.springmimic.model;

import com.iimmersao.springmimic.annotations.*;

@Entity
@Table(name = "users")
public class MySqlUserEntity implements BaseUserEntity<Integer> {
    @Id
    @GeneratedValue
    @Column(name = "id")
    private Integer id;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    public MySqlUserEntity() {}

    public MySqlUserEntity(String username, String email) {
        this.username = username;
        this.email = email;
    }

    // Getters and setters
    public Integer getId() { return id; }

    @Override
    public void setId(Integer integer) {

    }

    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return String.format("User{id=%d, username='%s', email='%s'}", id, username, email);
    }
}
