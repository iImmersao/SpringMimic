package com.iimmersao.springmimic.core;

/*
import com.iimmersao.springmimic.annotations.Component;
import com.iimmersao.springmimic.annotations.Controller;
import com.iimmersao.springmimic.annotations.Service;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
        import java.util.stream.Collectors;

public class ComponentScanner {

    private final String basePackage;

    public ComponentScanner(String basePackage) {
        this.basePackage = basePackage;
    }

    public Set<Class<?>> scan() {
        return ClasspathScanner.findClasses(basePackage).stream()
                .filter(cls -> hasComponentAnnotation(cls))
                .collect(Collectors.toSet());
    }

    private boolean hasComponentAnnotation(Class<?> cls) {
        return cls.isAnnotationPresent(Component.class) ||
                cls.isAnnotationPresent(Controller.class) ||
                cls.isAnnotationPresent(Service.class);
    }
}
*/

/*
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.util.Set;
import java.util.stream.Collectors;

public class ComponentScanner {

    private final String basePackage;

    public ComponentScanner(String basePackage) {
        this.basePackage = basePackage;
    }

    public Set<Class<?>> scan() {
        try (ScanResult scanResult = new ClassGraph()
                .enableAllInfo()
                .acceptPackages(basePackage)
                .scan()) {

            return scanResult.getClassesWithAnnotation("annotations.Component")
                    .loadClasses().stream()
                    .collect(Collectors.toSet());
        }
    }
}
*/

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
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

            Set<Class<?>> componentClasses = scanResult.getClassesWithAnnotation(Component.class.getName())
                    .loadClasses()
                    .stream()
                    .collect(Collectors.toSet());

            Set<Class<?>> controllerClasses = scanResult.getClassesWithAnnotation(Controller.class.getName())
                    .loadClasses()
                    .stream()
                    .collect(Collectors.toSet());

            // Combine them into one set
            componentClasses.addAll(controllerClasses);
            return componentClasses;
        }
    }
}
