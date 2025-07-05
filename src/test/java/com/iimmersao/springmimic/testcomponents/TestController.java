package com.iimmersao.springmimic.testcomponents;

import com.iimmersao.springmimic.annotations.*;
import com.iimmersao.springmimic.controllers.UserController;
import com.iimmersao.springmimic.model.User;
import com.iimmersao.springmimic.web.PageRequest;

import java.util.List;

@Controller
public class TestController {

    public static class User {
        private String name;
        private int age;

        // Getters and setters required for Jackson
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

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
    public String createUser(@RequestBody User user) {
        return "Created user: " + user.getName();
    }

    @PutMapping("/users/{id}")
    public String updateUser(@PathVariable("id") String id, @RequestBody User user) {
        return "Updated user " + id + " with name " + user.getName();
    }

    @PatchMapping("/users/{id}")
    public String patchUser(@PathVariable("id") String id, @RequestBody User user) {
        return "Patched user " + id + " with name " + user.getName();
    }

    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable("id") String id) {
        return "Deleted user " + id;
    }
}
