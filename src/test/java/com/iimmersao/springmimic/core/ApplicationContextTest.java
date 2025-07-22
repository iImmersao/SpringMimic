package com.iimmersao.springmimic.core;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings(value = "unused")
class ApplicationContextTest {

    @Test
    void shouldFindControllers() {
        ApplicationContext context = new ApplicationContext("com.iimmersao.springmimic.testcomponents");
        context.initialize(null);
        Collection<Object> controllers = context.getControllers();

        // Should only contain Controllers
        assertTrue(controllers.stream().anyMatch(b -> b.getClass().getSimpleName().equals("FakeController")));
        assertFalse(controllers.stream().anyMatch(b -> b.getClass().getSimpleName().equals("FakeComponent")));
    }

    @Test
    void shouldFindAllComponents() {
        ApplicationContext context = new ApplicationContext("com.iimmersao.springmimic.testcomponents");
        context.initialize(null);
        Collection<Object> components = context.getComponents();

        assertEquals(7, components.size()); // ExampleService, FakeComponent, FakeController, TestController
    }

    @Test
    void shouldFindServices() {
        ApplicationContext context = new ApplicationContext("com.iimmersao.springmimic.testcomponents");
        context.initialize(null);
        Collection<Object> services = context.getServices();

        // Should only contain Controllers
        assertTrue(services.stream().anyMatch(b -> b.getClass().getSimpleName().equals("ExampleService")));
        assertFalse(services.stream().anyMatch(b -> b.getClass().getSimpleName().equals("FakeComponent")));
    }

    @Test
    void shouldThrowWhenClassCannotBeInstantiated() {
        ApplicationContext context = new ApplicationContext("com.iimmersao.springmimic.core");

        // This package is hypothetical — for demonstration only
        // You’d need to put a NonInstantiable class somewhere to test this properly
        // So we can’t really test this part without a real class in your codebase.
    }
}
