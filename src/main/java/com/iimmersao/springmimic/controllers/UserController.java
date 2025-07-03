package com.iimmersao.springmimic.controllers;

import com.iimmersao.springmimic.annotations.*;
import com.iimmersao.springmimic.client.RestClient;
import com.iimmersao.springmimic.model.User;
import com.iimmersao.springmimic.services.UserService;

import java.util.List;
import java.util.Optional;

@Controller
public class UserController {

    @Inject
    RestClient restClient;

    @Inject
    private UserService userService;

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

    @PostMapping("/users")
    public String createUser(@RequestBody User user) {
        User createdUser = userService.save(user);
        return "User created with ID: " + createdUser.getId();
    }

    @GetMapping("/users/{id}")
    public String getUserById(@PathVariable("id") String id) {
        Optional<User> user = userService.findById(id);
        return user.map(u -> "User: " + u.getUsername())
                .orElse("User not found");
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userService.findAll();
    }

    @PutMapping("/users/{id}")
    public String updateUser(@PathVariable("id") String id, @RequestBody User updated) {
        updated.setId(id);
        userService.update(updated);
        return "User updated with ID: " + id;
    }

    @PatchMapping("/users/{id}")
    public String patchUser(@PathVariable("id") String id, @RequestBody User updated) {
        updated.setId(id);
        userService.update(updated);
        return "User patched with ID: " + id;
    }

    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable("id") String id) {
        userService.deleteById(id);
        return "User deleted with ID: " + id;
    }

    @DeleteMapping("/users")
    public String deleteAllUsers() {
        userService.deleteAll();
        return "All users deleted";
    }

}

