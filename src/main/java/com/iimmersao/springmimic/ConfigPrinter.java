package com.iimmersao.springmimic;

import com.iimmersao.springmimic.annotations.*;

@Controller
public class ConfigPrinter {

    @Value("app.name")
    private String appName;

    @Value("app.port")
    private int port;

    @GetMapping("/config")
    public String showConfig() {
        return "App: " + appName + ", Port: " + port;
    }
}
