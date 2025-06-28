package com.iimmersao.springmimic.core;

import com.iimmersao.springmimic.annotations.*;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.util.*;
import java.util.stream.Collectors;

public class ApplicationContext {
    private final String basePackage;

    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;
    }

    public Set<Object> getControllers() {
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackages(basePackage)
                .scan()) {

            Set<Class<?>> componentClasses = new HashSet<>();
            componentClasses.addAll(scanResult.getClassesWithAnnotation(Controller.class.getName()).loadClasses());
            componentClasses.addAll(scanResult.getClassesWithAnnotation(Component.class.getName()).loadClasses());

            return componentClasses.stream()
                    .map(this::instantiate)
                    .collect(Collectors.toSet());
        }
    }

    private Object instantiate(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate: " + clazz.getName(), e);
        }
    }
}
