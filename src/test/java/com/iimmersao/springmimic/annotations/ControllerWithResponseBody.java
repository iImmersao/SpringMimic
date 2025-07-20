package com.iimmersao.springmimic.annotations;

import com.iimmersao.springmimic.model.H2User;

@Controller
@ResponseBody
public class ControllerWithResponseBody {

    @GetMapping("/respbodyplaintext")
    public String plainText() {
        return "hello";
    }

    @GetMapping("/respbodyjson")
    public H2User getUser() {
        H2User result = new H2User();
        result.setId(42);
        result.setUsername("Alice");
        result.setEmail("alice@example.com");
        return result;
    }

    @GetMapping("/respbodynoannotation")
    public H2User withoutAnnotation() {
        H2User result = new H2User();
        result.setId(42);
        result.setUsername("Alice");
        result.setEmail("alice@example.com");
        return result;
    }
}
