package com.iimmersao.springmimic.annotations;

import com.iimmersao.springmimic.model.H2User;

@Controller
public class ResponseBodyController {

    @GetMapping("/plaintext")
    @ResponseBody
    public String plainText() {
        return "hello";
    }

    @GetMapping("/json")
    @ResponseBody
    public H2User getUser() {
        H2User result = new H2User();
        result.setId(42);
        result.setUsername("Alice");
        result.setEmail("alice@example.com");
        return result;
    }

    @GetMapping("/noannotation")
    public H2User withoutAnnotation() {
        H2User result = new H2User();
        result.setId(42);
        result.setUsername("Alice");
        result.setEmail("alice@example.com");
        return result;
    }
}
