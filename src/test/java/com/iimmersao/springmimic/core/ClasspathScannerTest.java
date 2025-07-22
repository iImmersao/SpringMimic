package com.iimmersao.springmimic.core;

import com.iimmersao.springmimic.testcomponents.ScannedClass;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings(value = "unused")
class ClasspathScannerTest {

    @Test
    void shouldFindClassesInPackage() {
        Set<Class<?>> classes = ClasspathScanner.findClasses("com.iimmersao.springmimic.testcomponents");

        assertTrue(classes.contains(ScannedClass.class),
                "Scanned classes should include ScannedClass");
    }
}
