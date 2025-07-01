package com.iimmersao.springmimic.controllers;

import com.iimmersao.springmimic.annotations.*;
import com.iimmersao.springmimic.client.RestClient;

@Controller
public class UserController {

    @Inject
    RestClient restClient;

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

    @PutMapping("/users/{id}")
    public String updateUser(@PathVariable("id") String id, @RequestBody User user) {
        return "Updated user " + id + " with name " + user.name;
    }

    @PatchMapping("/users/{id}")
    public String patchUser(@PathVariable("id") String id, @RequestBody User user) {
        return "Patched user " + id + " with name " + user.name;
    }

    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable("id") String id) {
        return "Deleted user " + id;
    }

    @GetMapping("/external")
    public String callExternalService() throws Exception {
        // For example, proxying a public JSON API
        Post post = restClient.get("https://jsonplaceholder.typicode.com/posts/1", Post.class);
        return "Fetched post: " + post.title;
    }

    public static class Post {
        public int userId;
        public int id;
        public String title;
        public String body;
    }
}

