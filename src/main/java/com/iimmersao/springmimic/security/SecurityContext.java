package com.iimmersao.springmimic.security;

public class SecurityContext {
    private static AuthManager authManager;

    public static void setAuthManager(AuthManager manager) {
        authManager = manager;
    }

    public static AuthManager getAuthManager() {
        return authManager;
    }
}
