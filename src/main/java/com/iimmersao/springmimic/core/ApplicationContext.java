package com.iimmersao.springmimic.core;

import com.iimmersao.springmimic.annotations.*;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class ApplicationContext {
    private final String basePackage;

    private final Map<Class<?>, Object> components = new HashMap<>();
    private final Map<Class<?>, Object> controllers = new HashMap<>();
    private final Map<Class<?>, Object> beans = new HashMap<>();
    private final Map<Class<?>, Object> entities = new HashMap<>();
    private final Map<Class<?>, Object> services = new HashMap<>();

    ComponentScanner scanner;

    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;

        this.scanner = new ComponentScanner(basePackage);
        registerByAnnotation(scanner, basePackage, Component.class, components);
        registerByAnnotation(scanner, basePackage, Controller.class, controllers);
        registerByAnnotation(scanner, basePackage, Bean.class, beans);
        registerByAnnotation(scanner, basePackage, Entity.class, entities);
        registerByAnnotation(scanner, basePackage, Service.class, services);
    }

    private void registerByAnnotation(ComponentScanner scanner, String basePackage,
                                      Class<? extends Annotation> annotation,
                                      Map<Class<?>, Object> registry) {
        Set<Class<?>> classes = scanner.scanByAnnotation(basePackage, annotation);
        for (Class<?> clazz : classes) {
            Object instance = createInstance(clazz); // With injection!
            registry.put(clazz, instance);
        }
    }


    /*
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
     */

    public Map<Class<?>, Object> getControllers() {
        return this.controllers;
    }

    public Map<Class<?>, Object> getComponents() {
        return this.components;
    }

    public Map<Class<?>, Object> getBeans() {
        return beans;
    }

    public Map<Class<?>, Object> getEntities() {
        return entities;
    }

    public Map<Class<?>, Object> getServices() {
        return services;
    }

    private Object instantiate(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate: " + clazz.getName(), e);
        }
    }

    private Object createInstance(Class<?> clazz) {
        try {
            // Create a new instance of the class using its no-arg constructor
            Object instance = clazz.getDeclaredConstructor().newInstance();

            // Inject dependencies into fields annotated with @Inject or @Autowired
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class) || field.isAnnotationPresent(Autowired.class)) {
                    Class<?> fieldType = field.getType();
                    Object dependency = getBean(fieldType);

                    if (dependency == null) {
                        // Attempt to recursively create the dependency
                        dependency = createInstance(fieldType);
                        registerInstance(fieldType, dependency); // Register in correct map
                    }

                    field.setAccessible(true);
                    field.set(instance, dependency);
                }
            }

            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
        }
    }

    private void registerInstance(Class<?> type, Object instance) {
        if (type.isAnnotationPresent(Controller.class)) {
            controllers.put(type, instance);
        } else if (type.isAnnotationPresent(Component.class)) {
            components.put(type, instance);
        } else if (type.isAnnotationPresent(Bean.class)) {
            beans.put(type, instance);
        } else if (type.isAnnotationPresent(Entity.class)) {
            entities.put(type, instance);
        } else {
            // If it’s not annotated, default to components map
            components.put(type, instance);
        }
    }

    private Object getBean(Class<?> type) {
        Object instance = components.get(type);
        if (instance == null) instance = controllers.get(type);
        if (instance == null) instance = beans.get(type);
        if (instance == null) instance = entities.get(type);
        return instance;
    }
}
