package com.iimmersao.springmimic.controllers;

import com.iimmersao.springmimic.annotations.Authenticated;
import com.iimmersao.springmimic.annotations.Controller;
import com.iimmersao.springmimic.annotations.GetMapping;
import com.iimmersao.springmimic.annotations.RolesAllowed;

@Controller
@SuppressWarnings(value = "unused")
public class SecureController {

    @GetMapping("/secure")
    @Authenticated
    @RolesAllowed({"ROLE_USER"})
    public String secureEndpoint() {
        return "You are authenticated!";
    }

    @GetMapping("/admin")
    @Authenticated
    @RolesAllowed({"ROLE_ADMIN"})
    public String adminOnly() {
        return "Hello admin!";
    }
}
