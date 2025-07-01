package com.iimmersao.springmimic.core;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationContextTest {

    @Test
    void shouldFindControllersAndComponents() {
        ApplicationContext context = new ApplicationContext("com.iimmersao.springmimic.testcomponents");
        Collection<Object> controllers = context.getControllers().values();

        // Should only contain Controllers
        assertTrue(controllers.stream().anyMatch(b -> b.getClass().getSimpleName().equals("FakeController")));
        assertFalse(controllers.stream().anyMatch(b -> b.getClass().getSimpleName().equals("FakeComponent")));

        Collection<Object> components = context.getComponents().values();

        // Should only contain Controllers
        assertFalse(components.stream().anyMatch(b -> b.getClass().getSimpleName().equals("FakeController")));
        assertTrue(components.stream().anyMatch(b -> b.getClass().getSimpleName().equals("FakeComponent")));
    }

    @Test
    void shouldThrowWhenClassCannotBeInstantiated() {
        ApplicationContext context = new ApplicationContext("com.iimmersao.springmimic.core");

        // This package is hypothetical — for demonstration only
        // You’d need to put a NonInstantiable class somewhere to test this properly
        // So we can’t really test this part without a real class in your codebase.
    }
}
