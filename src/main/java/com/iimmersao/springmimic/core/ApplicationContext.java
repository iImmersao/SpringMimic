package com.iimmersao.springmimic.core;

import com.iimmersao.springmimic.annotations.Autowired;
import com.iimmersao.springmimic.annotations.Component;
import com.iimmersao.springmimic.annotations.Controller;
import com.iimmersao.springmimic.annotations.Repository;
import com.iimmersao.springmimic.annotations.Service;
import com.iimmersao.springmimic.core.ComponentScanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class ApplicationContext {

    private final String basePackage;
    private final Map<Class<?>, Object> manualBeans = new HashMap<>();
    private final Map<Class<?>, Object> components = new HashMap<>();

    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;
    }

    public <T> void registerBean(Class<T> type, T instance) {
        manualBeans.put(type, instance);
    }

    public void initialize() {
        Set<Class<?>> discoveredClasses = new ComponentScanner(basePackage).scan();

        for (Class<?> clazz : discoveredClasses) {
            if (isComponentClass(clazz)) {
                Object instance = createInstance(clazz);
                components.put(clazz, instance);
            }
        }

        for (Object instance : components.values()) {
            injectDependencies(instance);
        }
    }

    private boolean isComponentClass(Class<?> clazz) {
        return clazz.isAnnotationPresent(Component.class)
                || clazz.isAnnotationPresent(Controller.class)
                || clazz.isAnnotationPresent(Service.class)
                || clazz.isAnnotationPresent(Repository.class);
    }

    private Object createInstance(Class<?> clazz) {
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            injectDependencies(instance);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate: " + clazz.getName(), e);
        }
    }

    private void injectDependencies(Object instance) {
        for (Field field : instance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                Object dependency = resolveDependency(field.getType());
                if (dependency == null) {
                    throw new RuntimeException("No bean found for type: " + field.getType().getName());
                }

                field.setAccessible(true);
                try {
                    field.set(instance, dependency);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to inject field: " + field.getName(), e);
                }
            }
        }
    }

    private Object resolveDependency(Class<?> type) {
        // First, try exact match from manual beans
        Object bean = manualBeans.get(type);
        if (bean != null) return bean;

        // Look for assignable types in manual beans
        for (Map.Entry<Class<?>, Object> entry : manualBeans.entrySet()) {
            if (type.isAssignableFrom(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Then try components
        for (Map.Entry<Class<?>, Object> entry : components.entrySet()) {
            if (type.isAssignableFrom(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    public <T> T getBean(Class<T> type) {
        Object bean = resolveDependency(type);
        if (bean == null) {
            throw new RuntimeException("No bean found for type: " + type.getName());
        }
        return type.cast(bean);
    }

    public Collection<Object> getAllControllers() {
        List<Object> controllers = new ArrayList<>();
        for (Object obj : components.values()) {
            if (obj.getClass().isAnnotationPresent(Controller.class)) {
                controllers.add(obj);
            }
        }
        return controllers;
    }

    public List<Object> getControllers() {
        return components.values().stream()
                .filter(bean -> bean.getClass().isAnnotationPresent(Controller.class))
                .collect(Collectors.toList());
    }

    public Collection<Object> getComponents() {
        Map<Class<?>, Object> allBeans = new HashMap<>();
        allBeans.putAll(manualBeans);
        allBeans.putAll(components);
        return allBeans.values();
    }

    public Collection<Object> getServices() {
        return getBeansWithAnnotation(Service.class);
    }

    public Collection<Object> getRepositories() {
        return getBeansWithAnnotation(Repository.class);
    }

    private Collection<Object> getBeansWithAnnotation(Class<? extends Annotation> annotationClass) {
        Map<Class<?>, Object> allBeans = new HashMap<>();
        allBeans.putAll(manualBeans);
        allBeans.putAll(components);

        return allBeans.entrySet().stream()
                .filter(entry -> entry.getKey().isAnnotationPresent(annotationClass))
                .map(Map.Entry::getValue)
                .toList();
    }
}
