package com.iimmersao.springmimic.repository;

import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.web.PageRequest;

import java.lang.reflect.Method;
import java.util.*;
import java.lang.reflect.InvocationHandler;

public class RepositoryInvocationHandler implements InvocationHandler {
    private final DatabaseClient client;
    private final Class<?> entityType;

    public RepositoryInvocationHandler(DatabaseClient client, Class<?> entityType, Class<?> repositoryType) {
        this.client = client;
        this.entityType = entityType;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();

        // Handle standard methods
        switch (name) {
            case "findById":
                return client.findById(entityType, args[0]);
            case "findAll":
                if (args != null && args.length > 0 && args[0] instanceof PageRequest) {
                    return client.findAll(entityType, (PageRequest) args[0]);
                } else {
                    return client.findAll(entityType);
                }
            case "save":
                Object arg = args[0];
                if (arg == null) {
                    throw new IllegalArgumentException("Cannot save null entity");
                }
                if (!entityType.isInstance(arg)) {
                    throw new IllegalArgumentException("Expected instance of " + entityType.getName() + " but got " + arg.getClass().getName());
                }
                client.save(arg);
                return null;
            case "deleteById":
                client.deleteById(entityType, args[0]);
                return null;
            case "deleteAll":
                client.deleteAll(entityType);
                return null;
        }

        // Handle dynamic methods like findByTitle, findByTitleContains
        String methodName = method.getName();

        if (methodName.startsWith("findBy")) {
            return handleFindBy(method, args);
        } else if (methodName.startsWith("existsBy")) {
            return handleExistsBy(methodName, args);
        } else if (methodName.startsWith("countBy")) {
            return handleCountBy(methodName, args);
        }

        throw new UnsupportedOperationException("Unsupported method: " + method.getName());
    }

    private String decapitalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return Character.toLowerCase(input.charAt(0)) + input.substring(1);
    }

    private Object handleFindBy(Method method, Object[] args) {
        String methodName = method.getName();
        String prefix = "findBy";
        String[] fields;

        PageRequest pageRequest = new PageRequest();
        Map<String, Object> filters = new HashMap<>();

        if (methodName.contains("Contains")) {
            // Handle contains query, e.g., findByTitleContains
            fields = methodName
                    .substring(prefix.length(), methodName.indexOf("Contains"))
                    .split("And");

            for (int i = 0; i < fields.length; i++) {
                String field = decapitalize(fields[i]);
                Object value = args[i];
                filters.put(field, "%" + value + "%");
                pageRequest.addLikeField(field);
            }
        } else {
            // Handle exact match, e.g., findByTitle
            fields = methodName
                    .substring(prefix.length())
                    .split("And");

            for (int i = 0; i < fields.length; i++) {
                String field = decapitalize(fields[i]);
                Object value = args[i];
                filters.put(field, value);
            }
        }

        pageRequest.setFilters(filters);

        List<?> results = client.findAll(entityType, pageRequest);

        // Cast to correct return type
        Class<?> returnType = method.getReturnType();

        if (List.class.isAssignableFrom(returnType)) {
            return results;
        } else if (!results.isEmpty()) {
            return results.getFirst();
        } else {
            return null;
        }
    }

    private Object handleExistsBy(String methodName, Object[] args) {
        String field = decapitalize(methodName.substring("existsBy".length()));
        Map<String, Object> filter = Map.of(field, args[0]);

        PageRequest pr = new PageRequest();
        pr.setFilters(filter);

        List<?> results = client.findAll(entityType, pr);
        return !results.isEmpty();
    }

    private Object handleCountBy(String methodName, Object[] args) {
        String field = decapitalize(methodName.substring("countBy".length()));
        Map<String, Object> filter = Map.of(field, args[0]);

        PageRequest pr = new PageRequest();
        pr.setFilters(filter);

        List<?> results = client.findAll(entityType, pr);
        return results.size();  // Consider a real count() method later
    }

    private String lowerFirst(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
