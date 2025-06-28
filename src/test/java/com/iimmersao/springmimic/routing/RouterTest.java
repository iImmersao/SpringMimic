package com.iimmersao.springmimic.routing;

import com.iimmersao.springmimic.annotations.GetMapping;
import com.iimmersao.springmimic.annotations.PathVariable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

import com.iimmersao.springmimic.routing.Router;
import com.iimmersao.springmimic.routing.RouteHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class RouterTest {

    static class TestController {
        @GetMapping("/users/{id}")
        public String getUser(String id) {
            return "User ID: " + id;
        }

        @GetMapping("/posts/{postId}/comments/{commentId}")
        public String getComment(String postId, String commentId) {
            return "Post " + postId + ", Comment " + commentId;
        }
    }

    private Router router;

    @BeforeEach
    void setUp() {
        router = new Router();
        Set<Object> controllers = Set.of(new TestController());
        router.registerControllers(controllers);
    }

    @Test
    void shouldMatchPathWithSingleVariable() {
        String method = "GET";
        String uri = "/users/abc123";

        var handlerOpt = router.findHandler(method, uri);
        assertTrue(handlerOpt.isPresent(), "Expected route to be found for /users/{id}");
    }

    @Test
    void shouldMatchPathWithMultipleVariables() {
        String method = "GET";
        String uri = "/posts/99/comments/123";

        var handlerOpt = router.findHandler(method, uri);
        assertTrue(handlerOpt.isPresent(), "Expected route to be found for /posts/{postId}/comments/{commentId}");
    }

    @Test
    void shouldNotMatchUnknownRoute() {
        String method = "GET";
        String uri = "/unknown/path";

        var handlerOpt = router.findHandler(method, uri);
        assertFalse(handlerOpt.isPresent(), "Expected no route to be found for /unknown/path");
    }
}
