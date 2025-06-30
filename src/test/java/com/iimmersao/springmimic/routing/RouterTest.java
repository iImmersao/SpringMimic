package com.iimmersao.springmimic.routing;

import com.iimmersao.springmimic.annotations.GetMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

        RouteMatch match = router.findHandler("GET", uri);
        assertNotNull(match, "Expected route to be found for /users/{id}");
    }

    @Test
    void shouldMatchPathWithMultipleVariables() {
        String method = "GET";
        String uri = "/posts/99/comments/123";

        RouteMatch match = router.findHandler("GET", uri);
        assertNotNull(match, "Expected route to be found for /posts/{postId}/comments/{commentId}");
    }

    @Test
    void shouldNotMatchUnknownRoute() {
        String method = "GET";
        String uri = "/unknown/path";

        RouteMatch match = router.findHandler("GET", uri);
        assertNull(match, "Expected no route to be found for /unknown/path");
    }
}
