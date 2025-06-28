package com.iimmersao.springmimic.routing;

import com.iimmersao.springmimic.annotations.GetMapping;
import com.iimmersao.springmimic.annotations.PathVariable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RouterTest {

    private Router router;

    static class DummyController {
        @GetMapping("/hello")
        public String hello() {
            return "Hello!";
        }

        @GetMapping("/users/{id}")
        public String getUser(@PathVariable("id") String id) {
            return "User " + id;
        }
    }

    @BeforeEach
    void setUp() {
        router = new Router();
        router.registerControllers(Set.of(new DummyController()));
    }

    @Test
    void shouldMatchExactPath() {
        Optional<RouteHandler> handler = router.findHandler("GET", "/hello");
        assertTrue(handler.isPresent());
    }

    @Test
    void shouldMatchPathWithVariable() {
        Optional<RouteHandler> handlerOpt = router.findHandler("GET", "/users/42");
        assertTrue(handlerOpt.isPresent());

        RouteHandler handler = handlerOpt.get();
        assertEquals("42", handler.getPathVariables().get("id"));
    }

    @Test
    void shouldRejectUnregisteredMethod() {
        Optional<RouteHandler> handler = router.findHandler("POST", "/hello");
        assertTrue(handler.isEmpty());
    }

    @Test
    void shouldRejectUnknownPath() {
        Optional<RouteHandler> handler = router.findHandler("GET", "/unknown");
        assertTrue(handler.isEmpty());
    }
}
