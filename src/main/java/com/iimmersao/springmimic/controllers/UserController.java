package com.iimmersao.springmimic.controllers;

import com.iimmersao.springmimic.annotations.*;
import com.iimmersao.springmimic.client.RestClient;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.model.User;

import java.util.List;
import java.util.Optional;

@Controller
public class UserController {

    @Inject
    RestClient restClient;

    @Autowired
    private DatabaseClient dbClient;

    /*
    public static class User {
        private String name;
        private int age;

        // Getters and setters required for Jackson
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }
     */

    /*
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
     */

    /*
    @PostMapping("/users")
    public String createUser(@RequestBody User user) {
        return "Created: " + user.getUsername() + ", Age: " + user.getEmail();
    }
     */

    /*
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
        return "Updated user " + id + " with name " + user.getUsername();
    }

    @PatchMapping("/users/{id}")
    public String patchUser(@PathVariable("id") String id, @RequestBody User user) {
        return "Patched user " + id + " with name " + user.getUsername();
    }

    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable("id") String id) {
        return "Deleted user " + id;
    }
     */

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
        dbClient.save(user);
        return "User created with ID: " + user.getId();
    }

    @GetMapping("/users/{id}")
    public String getUserById(@PathVariable("id") Integer id) {
        Optional<User> user = dbClient.findById(User.class, id);
        return user.map(u -> "User: " + u.getUsername())
                .orElse("User not found");
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return dbClient.findAll(User.class);
    }

    @PutMapping("/users/{id}")
    public String updateUser(@PathVariable("id") Integer id, @RequestBody User updated) {
        updated.setId(id);
        dbClient.updateById(updated);
        return "User updated with ID: " + id;
    }

    @PatchMapping("/users/{id}")
    public String patchUser(@PathVariable("id") Integer id, @RequestBody User updated) {
        updated.setId(id);
        dbClient.updateById(updated);
        return "User patched with ID: " + id;
    }

    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable("id") Integer id) {
        dbClient.deleteById(User.class, id);
        return "User deleted with ID: " + id;
    }

    @DeleteMapping("/users")
    public String deleteAllUsers() {
        dbClient.deleteAll(User.class);
        return "All users deleted";
    }

}

