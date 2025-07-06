package com.iimmersao.springmimic.security;

import java.util.Optional;

public interface Authenticator {
    UserDetails authenticate(String username, String password);
}
