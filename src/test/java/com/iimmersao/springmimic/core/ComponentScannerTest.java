package com.iimmersao.springmimic.core;


import com.iimmersao.springmimic.testcomponents.ExampleService;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ComponentScannerTest {

    @Test
    void shouldFindComponentAnnotatedClasses() {
        ComponentScanner scanner = new ComponentScanner("com.iimmersao.springmimic.testcomponents");
        Set<Class<?>> components = scanner.scan();

        assertTrue(
                components.contains(ExampleService.class),
                "ComponentScanner should find ExampleService class"
        );
    }
}
