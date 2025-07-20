package com.iimmersao.springmimic.core;

import com.iimmersao.springmimic.annotations.*;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

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

            Set<Class<?>> restControllerClasses = new HashSet<>(scanResult.getClassesWithAnnotation(RestController.class.getName())
                    .loadClasses());

            Set<Class<?>> serviceClasses = new HashSet<>(scanResult.getClassesWithAnnotation(Service.class.getName())
                    .loadClasses());

            Set<Class<?>> repositoryClasses = new HashSet<>(scanResult.getClassesWithAnnotation(Repository.class.getName())
                    .loadClasses());

            // Combine them into one set
            componentClasses.addAll(controllerClasses);
            componentClasses.addAll(restControllerClasses);
            componentClasses.addAll(serviceClasses);
            componentClasses.addAll(repositoryClasses);
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
