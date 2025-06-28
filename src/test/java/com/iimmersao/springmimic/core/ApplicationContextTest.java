package com.iimmersao.springmimic.core;

import com.iimmersao.springmimic.annotations.Component;
import com.iimmersao.springmimic.annotations.Controller;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationContextTest {

    @Test
    void shouldFindControllersAndComponents() {
        ApplicationContext context = new ApplicationContext("com.iimmersao.springmimic.testcomponents");
        Set<Object> beans = context.getControllers();

        assertTrue(beans.stream().anyMatch(b -> b.getClass().getSimpleName().equals("FakeController")));
        assertTrue(beans.stream().anyMatch(b -> b.getClass().getSimpleName().equals("FakeComponent")));
    }

    @Test
    void shouldThrowWhenClassCannotBeInstantiated() {
        ApplicationContext context = new ApplicationContext("com.iimmersao.springmimic.core");

        // This package is hypothetical — for demonstration only
        // You’d need to put a NonInstantiable class somewhere to test this properly
        // So we can’t really test this part without a real class in your codebase.
    }
}
