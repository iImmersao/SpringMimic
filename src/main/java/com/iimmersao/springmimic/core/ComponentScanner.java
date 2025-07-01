package com.iimmersao.springmimic.core;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.iimmersao.springmimic.annotations.Component;
import com.iimmersao.springmimic.annotations.Controller;

public class ComponentScanner {

    private final String basePackage;

    public ComponentScanner(String basePackage) {
        this.basePackage = basePackage;
    }

    public Set<Class<?>> scan() {
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackages(basePackage)
                .scan()) {

            Set<Class<?>> componentClasses = new HashSet<>(scanResult.getClassesWithAnnotation(Component.class.getName())
                    .loadClasses());

            Set<Class<?>> controllerClasses = new HashSet<>(scanResult.getClassesWithAnnotation(Controller.class.getName())
                    .loadClasses());

            // Combine them into one set
            componentClasses.addAll(controllerClasses);
            return componentClasses;
        }
    }

    public Set<Class<?>> scanByAnnotation(String basePackage, Class<? extends Annotation> annotation) {
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackages(basePackage)
                .scan()) {

            return new HashSet<>(scanResult.getClassesWithAnnotation(annotation.getName())
                    .loadClasses());
        }
    }
}
