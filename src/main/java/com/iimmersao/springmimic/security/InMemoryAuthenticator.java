package com.iimmersao.springmimic.security;

import com.iimmersao.springmimic.annotations.Component;
import com.iimmersao.springmimic.exceptions.UnauthorizedException;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class InMemoryAuthenticator implements Authenticator {
    private final Map<String, Map<String, String>> users = Map.of(
            "admin", Map.of("password", "admin123", "role", "ROLE_ADMIN"),

            "user", Map.of("password", "user123", "role", "ROLE_USER")
    );

    @Override
    public UserDetails authenticate(String username, String password) {
        if (users.containsKey(username)) {
            Map<String, String> userDetails = users.get(username);
            if (userDetails.get("password").equals(password)) {
                String role = userDetails.get("role");
                return new UserDetails(username, Set.of(role));
            }
        }
        throw new UnauthorizedException("Invalid username or password");
    }
}
