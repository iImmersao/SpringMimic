package com.iimmersao.springmimic.controllers;

import com.iimmersao.springmimic.annotations.*;

@Controller
public class UserController {

    public static class User {
        private String name;
        private int age;

        // Getters and setters required for Jackson
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

    @GetMapping("/users/{id}")
    public String getUser(@PathVariable("id") String id,
                          @RequestParam("verbose") Boolean verbose,
                          @RequestParam("max") Integer max) {
        return "User ID: " + id + ", verbose=" + verbose + ", max=" + max;
    }

    @GetMapping("/posts/{postId}/comments/{commentId}")
    public String getComment(
            @PathVariable("postId") String postId,
            @PathVariable("commentId") String commentId) {
        return "Post: " + postId + ", Comment: " + commentId;
    }

    @PostMapping("/users")
    public String createUser(@RequestBody User user) {
        return "Created: " + user.getName() + ", Age: " + user.getAge();
    }

    @GetMapping("/users")
    public String listUsers() {
        return "User list here";
    }

    @GetMapping("/user/details")
    public String getUserDetails(@RequestParam("id") Integer id, @RequestParam("verbose") Boolean verbose) {
        boolean b = (verbose != null && verbose);
        return "Details for ID=" + id + ", verbose=" + b;
    }
}

