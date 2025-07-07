package com.iimmersao.springmimic.security;

public interface Authenticator {
    UserDetails authenticate(String username, String password);
}
