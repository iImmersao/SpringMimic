package com.iimmersao.springmimic.model;

import com.iimmersao.springmimic.annotations.*;

import java.util.Objects;

@Entity
@Table(name = "users")
public class H2User {
    @Id
    @GeneratedValue
    @Column(name = "id")
    private Integer id;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    public H2User() {
    }

    public H2User(Integer id, String username, String email, Boolean active, Integer age) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return String.format("H2User{id=%d, username='%s', email='%s'}",
                id, username, email);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof H2User h2User)) return false;
        return Objects.equals(id, h2User.id)
                && Objects.equals(username, h2User.username)
                && Objects.equals(email, h2User.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, email);
    }
}
