package com.iimmersao.springmimic.model;

import com.iimmersao.springmimic.annotations.*;

@Entity
@Table(name = "users")
public class UserDTO {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private String id;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    public UserDTO() {}

    public UserDTO(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public UserDTO(String id, String username, String email) {
        this.id = id;
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
