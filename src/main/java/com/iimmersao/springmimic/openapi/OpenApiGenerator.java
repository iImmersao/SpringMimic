package com.iimmersao.springmimic.openapi;

import com.iimmersao.springmimic.routing.RouteHandler;
import com.iimmersao.springmimic.routing.Router;

import java.util.*;

public class OpenApiGenerator {

    public static Map<String, Object> generateOpenApiSpec(Router router) {
        Map<String, Object> root = new LinkedHashMap<>();

        // Base info
        root.put("openapi", "3.0.0");

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", "API Documentation");
        info.put("version", "1.0.0");
        root.put("info", info);

        Map<String, Object> paths = new LinkedHashMap<>();

        for (Router.RouteEntry entry : router.getRoutes()) {
            String path = entry.path();
            String method = entry.httpMethod().toLowerCase(); // e.g. get, post, etc.
            RouteHandler handler = entry.handler();

            Map<String, Object> pathItem = (Map<String, Object>) paths
                    .computeIfAbsent(path, k -> new LinkedHashMap<>());

            Map<String, Object> operation = new LinkedHashMap<>();
            operation.put("summary", "Handler for " + entry.httpMethod() + " " + path);

            List<Map<String, Object>> parameters = new ArrayList<>();
            boolean hasRequestBody = false;

            for (MethodParameter param : handler.getMethodParameters()) {
                Map<String, Object> paramMap = new LinkedHashMap<>();

                if (param.isPathVariable()) {
                    paramMap.put("name", param.getName());
                    paramMap.put("in", "path");
                    paramMap.put("required", true);
                    paramMap.put("schema", typeSchema(param.getType()));
                    parameters.add(paramMap);
                } else if (param.isRequestParam()) {
                    paramMap.put("name", param.getName());
                    paramMap.put("in", "query");
                    paramMap.put("required", param.isRequired());
                    paramMap.put("schema", typeSchema(param.getType()));
                    parameters.add(paramMap);
                } else if (param.isRequestBody()) {
                    hasRequestBody = true;
                }
            }

            if (!parameters.isEmpty()) {
                operation.put("parameters", parameters);
            }

            if (hasRequestBody) {
                Map<String, Object> requestBody = new LinkedHashMap<>();
                requestBody.put("required", true);

                Map<String, Object> content = new LinkedHashMap<>();
                Map<String, Object> mediaType = new LinkedHashMap<>();
                mediaType.put("schema", Map.of("type", "object"));
                content.put("application/json", mediaType);

                requestBody.put("content", content);
                operation.put("requestBody", requestBody);
            }

            // Responses
            Map<String, Object> responses = new LinkedHashMap<>();
            Map<String, Object> success = new LinkedHashMap<>();
            success.put("description", "Successful response");
            responses.put("200", success);
            operation.put("responses", responses);

            pathItem.put(method, operation);
        }

        root.put("paths", paths);
        return root;
    }

    private static Map<String, Object> typeSchema(Class<?> type) {
        String typeName = "string"; // default

        if (Number.class.isAssignableFrom(type) || type.isPrimitive()) {
            if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
                typeName = "integer";
            } else if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
                typeName = "number";
            }
        } else if (type == boolean.class || type == Boolean.class) {
            typeName = "boolean";
        }

        return Map.of("type", typeName);
    }
}
