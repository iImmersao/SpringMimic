package com.iimmersao.springmimic.annotations;

import com.iimmersao.springmimic.model.H2User;

@Controller
@SuppressWarnings(value = "unused")
public class ResponseBodyController {

    @GetMapping("/plaintext/{name}")
    @ResponseBody
    public String plainText(@PathVariable("name") String name) {
        return "hello " + name;
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
