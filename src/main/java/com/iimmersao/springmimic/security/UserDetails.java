package com.iimmersao.springmimic.security;

import java.util.Set;

public class UserDetails {
    private final String username;
    private final Set<String> roles;

    public UserDetails(String username, Set<String> roles) {
        this.username = username;
        this.roles = roles;
    }

    public String getUsername() { return username; }
    public Set<String> getRoles() { return roles; }
}
