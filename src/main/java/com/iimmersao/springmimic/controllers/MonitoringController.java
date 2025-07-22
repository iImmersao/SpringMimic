package com.iimmersao.springmimic.controllers;

import com.iimmersao.springmimic.annotations.Controller;
import com.iimmersao.springmimic.annotations.GetMapping;

import java.util.Map;

@Controller
@SuppressWarnings(value = "unused")
public class MonitoringController {

    private final long startTime = System.currentTimeMillis();

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        Runtime runtime = Runtime.getRuntime();
        long uptime = System.currentTimeMillis() - startTime;

        return Map.of(
                "uptime", uptime,
                "heapMemoryUsed", runtime.totalMemory() - runtime.freeMemory(),
                "heapMemoryMax", runtime.maxMemory(),
                "availableProcessors", runtime.availableProcessors()
        );
    }

    @GetMapping("/info")
    public Map<String, String> info() {
        return Map.of(
                "app", "MyFramework",
                "version", "1.0.0",
                "buildTime", "2025-07-03"
        );
    }
}
