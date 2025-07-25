package com.iimmersao.springmimic.core;

import com.iimmersao.springmimic.annotations.*;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.repository.CrudRepository;
import com.iimmersao.springmimic.repository.RepositoryProxyFactory;
import com.iimmersao.springmimic.repository.RepositoryUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class ApplicationContext {

    private final String basePackage;
    private final Map<Class<?>, Object> manualBeans = new HashMap<>();
    private final Map<Class<?>, Object> components = new HashMap<>();

    private DatabaseClient dbClient;

    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;
    }

    public <T> void registerBean(Class<T> type, T instance) {
        manualBeans.put(type, instance);
    }

    public <T> void registerDatabaseBean(Class<T> type, T instance) {
        this.dbClient = (DatabaseClient) instance;
        registerBean(type, instance);
    }

    @SuppressWarnings("unchecked")
    <T> void registerRepository(Class<?> repositoryInterface, Object proxy) {
        registerBean((Class<T>) repositoryInterface, (T) proxy);
    }

    public void initialize(ComponentScanner appComponents) {
        Set<Class<?>> componentClasses = new ComponentScanner(basePackage).scan();
        if (appComponents != null) {
            componentClasses.addAll(appComponents.scan());
        }

        // Register @Repository interfaces via proxy
        for (Class<?> cls : componentClasses) {
            if (cls.isAnnotationPresent(Repository.class) && cls.isInterface()
                    && CrudRepository.class.isAssignableFrom(cls)) {
                Class<?> entityType = RepositoryUtils.extractEntityClass(cls);
                Object proxy = new RepositoryProxyFactory(dbClient).createRepository(cls, entityType);
                registerRepository(cls, proxy);
            }
        }

        for (Class<?> clazz : componentClasses) {
            // Skip classes already manually registered
            if (manualBeans.containsKey(clazz)) {
                continue;
            }

            if (isComponentClass(clazz)) {
                Object instance = createInstance(clazz);
                components.put(clazz, instance);
            }
        }
    }

    public void injectDependencies() {
        for (Object instance : components.values()) {
            injectDependencies(instance);
        }
    }

    private boolean isComponentClass(Class<?> clazz) {
        return clazz.isAnnotationPresent(Component.class)
                || clazz.isAnnotationPresent(Controller.class)
                || clazz.isAnnotationPresent(RestController.class)
                || clazz.isAnnotationPresent(Service.class)
                || clazz.isAnnotationPresent(Repository.class);
    }

    private Object createInstance(Class<?> clazz) {
        // 1. Check if instance already exists (in manually registered or scanned)
        if (manualBeans.containsKey(clazz)) {
            return manualBeans.get(clazz);
        }
        if (components.containsKey(clazz)) {
            return components.get(clazz);
        }

        try {
            Constructor<?> targetConstructor = getConstructor(clazz);

            // Resolve constructor parameters recursively
            Class<?>[] paramTypes = targetConstructor.getParameterTypes();
            Object[] paramInstances = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> paramType = paramTypes[i];

                Object paramInstance = manualBeans.getOrDefault(paramType, components.get(paramType));

                // Recursively create if not found
                if (paramInstance == null) {
                    paramInstance = createInstance(paramType);
                    components.put(paramType, paramInstance); // cache it
                }

                paramInstances[i] = paramInstance;
            }

            Object instance = targetConstructor.newInstance(paramInstances);
            components.put(clazz, instance); // cache this component too
            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of: " + clazz.getName(), e);
        }
    }

    private Constructor<?> getConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getConstructors();
        Constructor<?> targetConstructor = null;

        // Choose the constructor with the most parameters (simple strategy)
        for (Constructor<?> cons : constructors) {
            if (targetConstructor == null ||
                    cons.getParameterCount() > targetConstructor.getParameterCount()) {
                targetConstructor = cons;
            }
        }

        if (targetConstructor == null) {
            throw new IllegalStateException("No public constructor found for: " + clazz.getName());
        }
        return targetConstructor;
    }

    private void injectDependencies(Object instance) {
        for (Field field : instance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class) || field.isAnnotationPresent(Inject.class)) {
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
    
    public List<Object> getControllers() {
        return components.values().stream()
                .filter(bean -> (bean.getClass().isAnnotationPresent(Controller.class)
                            || bean.getClass().isAnnotationPresent(RestController.class))
                )
                .collect(Collectors.toList());
    }

    public Collection<Object> getComponents() {
        Map<Class<?>, Object> allBeans = new HashMap<>();
        allBeans.putAll(manualBeans);
        allBeans.putAll(components);
        return allBeans.values();
    }

    public void addComponents(ApplicationContext otherContext) {
        this.manualBeans.putAll(otherContext.manualBeans);
        this.manualBeans.putAll(otherContext.components);
    }

    public Collection<Object> getServices() {
        return getBeansWithAnnotation(Service.class);
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
