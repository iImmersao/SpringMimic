package com.iimmersao.springmimic.core;

import com.iimmersao.springmimic.annotations.PostConstruct;
import com.iimmersao.springmimic.annotations.PreDestroy;
import com.iimmersao.springmimic.annotations.Value;

import java.lang.reflect.*;
import java.util.*;

public class ApplicationContext {

    private final Map<Class<?>, Object> instances = new HashMap<>();
    private final ConfigLoader config;

    private final List<Object> shutdownHooks = new ArrayList<>();

    public ApplicationContext(Set<Class<?>> componentClasses, ConfigLoader config) {
        this.config = config;
        for (Class<?> clazz : componentClasses) {
            createBean(clazz);
        }

        // Inject config and run lifecycle
        for (Object bean : instances.values()) {
            injectConfig(bean);
            runPostConstruct(bean);
        }
    }

    private void runPostConstruct(Object bean) {
        for (Method method : bean.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                try {
                    method.setAccessible(true);
                    method.invoke(bean);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to invoke @PostConstruct: " + method, e);
                }
            }
        }
    }

    private Object createBean(Class<?> clazz) {
        if (instances.containsKey(clazz)) return instances.get(clazz);

        try {
            var constructor = clazz.getConstructors()[0];
            var dependencies = Arrays.stream(constructor.getParameterTypes())
                    .map(this::createBean)
                    .toArray();

            Object instance = constructor.newInstance(dependencies);
            instances.put(clazz, instance);

// If it has @PreDestroy, register for shutdown
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(PreDestroy.class)) {
                    shutdownHooks.add(instance);
                    break;
                }
            }
            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate: " + clazz.getName(), e);
        }
    }

    private void injectConfig(Object bean) {
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Value.class)) {
                String key = field.getAnnotation(Value.class).value();
                String value = config.get(key);
                if (value != null) {
                    field.setAccessible(true);
                    try {
                        field.set(bean, convert(value, field.getType()));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Failed to inject config: " + key, e);
                    }
                }
            }
        }
    }

    private Object convert(String value, Class<?> type) {
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        throw new RuntimeException("Unsupported config type: " + type.getName());
    }

    public <T> T getBean(Class<T> clazz) {
        return clazz.cast(instances.get(clazz));
    }

    public Collection<Object> getAllBeans() {
        return instances.values();
    }

    public void shutdown() {
        for (Object bean : shutdownHooks) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(PreDestroy.class)) {
                    try {
                        method.setAccessible(true);
                        method.invoke(bean);
                    } catch (Exception e) {
                        System.err.println("Failed @PreDestroy: " + method.getName());
                    }
                }
            }
        }
    }
}
