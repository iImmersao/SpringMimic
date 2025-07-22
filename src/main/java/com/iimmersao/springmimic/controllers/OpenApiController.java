package com.iimmersao.springmimic.controllers;

import com.iimmersao.springmimic.annotations.Controller;
import com.iimmersao.springmimic.annotations.GetMapping;
import com.iimmersao.springmimic.annotations.Inject;
import com.iimmersao.springmimic.annotations.Produces;
import com.iimmersao.springmimic.openapi.OpenApiGenerator;
import com.iimmersao.springmimic.routing.Router;

import java.util.Map;

@Controller
@SuppressWarnings(value = "unused")
public class OpenApiController {

    @Inject
    private Router router;

    @GetMapping("/openapi.json")
    @Produces("application/json")
    public Map<String, Object> getOpenApiSpec() {
        return OpenApiGenerator.generateOpenApiSpec(router);
    }
}
