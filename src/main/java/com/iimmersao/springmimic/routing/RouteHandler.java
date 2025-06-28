package com.iimmersao.springmimic.routing;

import com.iimmersao.springmimic.annotations.PathVariable;
import com.iimmersao.springmimic.annotations.RequestBody;
import com.iimmersao.springmimic.annotations.RequestParam;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteHandler {

    private final Object controllerInstance;
    private final Method method;
    private final Parameter[] parameters;

    private final String routePath;
    private final Pattern pattern;                 // Compiled regex for matching paths
    private final List<String> pathVariableNames;  // Names of variables in order

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RouteHandler(Object controllerInstance, Method method, String routePath) {
        this.controllerInstance = controllerInstance;
        this.method = method;
        this.parameters = method.getParameters();
        this.routePath = routePath;

        this.pathVariableNames = new ArrayList<>();
        this.pattern = compilePattern(routePath, pathVariableNames);
    }

    // Compiles route like /users/{id}/posts/{postId} into regex and records var names
    private Pattern compilePattern(String routePath, List<String> varNames) {
        StringBuilder regex = new StringBuilder();
        regex.append("^");

        // Split by '/', handle each segment
        String[] segments = routePath.split("/");
        for (String segment : segments) {
            if (segment.isEmpty()) continue;
            regex.append("/");

            if (segment.startsWith("{") && segment.endsWith("}")) {
                String varName = segment.substring(1, segment.length() - 1);
                varNames.add(varName);
                regex.append("([^/]+)");  // Capture group for variable
            } else {
                regex.append(Pattern.quote(segment));
            }
        }

        regex.append("$");
        return Pattern.compile(regex.toString());
    }

    // Checks if the URI matches this route's pattern
    public boolean matches(String uri) {
        return pattern.matcher(uri).matches();
    }

    // Extract path variables from URI into a map
    private Map<String, String> extractPathVariables(String uri) {
        Map<String, String> pathVars = new HashMap<>();
        Matcher matcher = pattern.matcher(uri);
        if (!matcher.matches()) return pathVars;

        for (int i = 0; i < pathVariableNames.size(); i++) {
            String name = pathVariableNames.get(i);
            String value = matcher.group(i + 1);
            pathVars.put(name, value);
        }
        return pathVars;
    }

    // Decode query string into map
    private Map<String, String> decodeQueryParams(String queryString) {
        Map<String, String> params = new HashMap<>();
        if (queryString == null || queryString.isEmpty()) return params;

        for (String pair : queryString.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                params.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
        }
        return params;
    }

    public Response handle(IHTTPSession session) {
        try {
            // Parse path variables
            String uri = session.getUri();
            Map<String, String> pathVariables = extractPathVariables(uri);

            // Parse query parameters or request body
            Map<String, String> queryParams = new HashMap<>();
            String requestBody = null;

            if (session.getMethod() == NanoHTTPD.Method.POST || session.getMethod() == NanoHTTPD.Method.PUT) {
                // Parse body into a temporary map (discarding files)
                //session.parseBody(new HashMap<>());
                Map<String, String> files = new HashMap<>();
                session.parseBody(files);
                requestBody = files.get("postData");                // Read body from temp file or from query string depending on your NanoHTTPD usage
                //requestBody = session.getQueryParameterString(); // adjust if needed to read raw body
                queryParams = decodeQueryParams(session.getQueryParameterString());
            } else {
                queryParams = decodeQueryParams(session.getQueryParameterString());
            }

            // Bind method parameters
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                Class<?> type = parameter.getType();

                if (parameter.isAnnotationPresent(PathVariable.class)) {
                    String name = parameter.getAnnotation(PathVariable.class).value();
                    String raw = pathVariables.get(name);
                    args[i] = convertValue(raw, type);

                } else if (parameter.isAnnotationPresent(RequestParam.class)) {
                    String name = parameter.getAnnotation(RequestParam.class).value();
                    String raw = queryParams.get(name);

                    if (raw == null) {
                        if (type.isPrimitive()) {
                            return NanoResponseHelper.badRequest("Missing required request parameter: " + name);
                        }
                        args[i] = null;
                    } else {
                        try {
                            args[i] = convertValue(raw, type);
                        } catch (Exception e) {
                            return NanoResponseHelper.badRequest("Invalid value for request parameter '" + name + "'");
                        }
                    }

                } else if (parameter.isAnnotationPresent(RequestBody.class)) {
                    if (requestBody == null) {
                        args[i] = null;
                    } else {
                        try {
                            args[i] = objectMapper.readValue(requestBody, type);
                        } catch (IOException e) {
                            return NanoResponseHelper.badRequest("Invalid request body: " + e.getMessage());
                        }
                    }

                } else {
                    args[i] = null;
                }
            }

            Object result = method.invoke(controllerInstance, args);
            return NanoResponseHelper.ok(result != null ? result.toString() : "");

        } catch (Exception e) {
            e.printStackTrace();
            return NanoResponseHelper.internalError("Internal server error: " + e.getMessage());
        }
    }

    private Object convertValue(String raw, Class<?> type) {
        if (raw == null) return null;
        if (type == String.class) return raw;

        if (type == int.class || type == Integer.class) return Integer.parseInt(raw);
        if (type == long.class || type == Long.class) return Long.parseLong(raw);

        if (type == boolean.class || type == Boolean.class) {
            if (!raw.equalsIgnoreCase("true") && !raw.equalsIgnoreCase("false")) {
                throw new IllegalArgumentException("Expected 'true' or 'false'");
            }
            return Boolean.parseBoolean(raw);
        }

        throw new IllegalArgumentException("Unsupported parameter type: " + type.getSimpleName());
    }

    // Simple helper to build NanoHTTPD responses
    static class NanoResponseHelper {
        static Response ok(String body) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "text/plain", body);
        }

        static Response badRequest(String message) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", message);
        }

        static Response internalError(String message) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", message);
        }
    }
}
