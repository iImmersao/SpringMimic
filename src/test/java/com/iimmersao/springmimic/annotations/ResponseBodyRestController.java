package com.iimmersao.springmimic.annotations;

import com.iimmersao.springmimic.model.H2User;

@RestController
public class ResponseBodyRestController {

    @GetMapping("/restplaintext")
    public String plainText() {
        return "hello";
    }

    @GetMapping("/restjson")
    public H2User getUser() {
        H2User result = new H2User();
        result.setId(42);
        result.setUsername("Alice");
        result.setEmail("alice@example.com");
        return result;
    }

    @GetMapping("/restnoannotation")
    @ResponseBody
    public H2User withoutAnnotation() {
        H2User result = new H2User();
        result.setId(42);
        result.setUsername("Alice");
        result.setEmail("alice@example.com");
        return result;
    }
}
