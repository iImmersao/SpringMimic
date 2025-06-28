package com.iimmersao.springmimic.testcomponents;

import com.iimmersao.springmimic.annotations.*;
import com.iimmersao.springmimic.controllers.UserController;

@Controller
public class TestController {

    @GetMapping("/echo/{value}")
    public String echo(@PathVariable("value") String val) {
        return "Echo: " + val;
    }

    @PostMapping("/json")
    public String postJson(@RequestBody TestData data) {
        return "Received: " + data.name;
    }

    public static class TestData {
        public String name;
    }

    @GetMapping("/user/details")
    public String userDetails(@RequestParam("id") String id,
                              @RequestParam("verbose") Boolean verbose,
                              @RequestParam("max") int max) {
        return "Details for ID=" + id + ", verbose=" + verbose;
    }

    @GetMapping("/posts/{postId}/comments/{commentId}")
    public String nestedPath(@PathVariable("postId") String postId,
                             @PathVariable("commentId") String commentId) {
        return "Post=" + postId + ", Comment=" + commentId;
    }

    @GetMapping("/users/{id}")
    public String getUser(@PathVariable("id") String id, @RequestParam("verbose") boolean verbose) {
        return "User ID: " + id + ", verbose=" + verbose;
    }

    @PostMapping("/users")
    public String createUser(@RequestBody UserController.User user) {
        return "Created user: " + user.getName();
    }
}
