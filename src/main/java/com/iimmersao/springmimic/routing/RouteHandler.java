package com.iimmersao.springmimic.routing;

import com.iimmersao.springmimic.annotations.PathVariable;
import com.iimmersao.springmimic.annotations.RequestBody;
import com.iimmersao.springmimic.annotations.RequestParam;
import com.iimmersao.springmimic.core.util.PathUtils;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteHandler {

    private String httpMethod;
    private final Object controllerInstance;
    private final Method method;
    private final Parameter[] parameters;

    private final String routePath;
    private final Pattern pattern;                 // Compiled regex for matching paths
    private final List<String> pathVariableNames;  // Names of variables in order

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RouteHandler(String httpMethod, String routePath, Object controllerInstance, Method method) {
        this.httpMethod = httpMethod;
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

    private Map<String, String> extractPathVariables(Matcher matcher) {
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Path pattern did not match URI");
        }

        Map<String, String> variables = new HashMap<>();
        List<String> paramNames = PathUtils.extractPathParamNames(routePath);

        try {
            for (int i = 0; i < paramNames.size(); i++) {
                String value = matcher.group(i + 1);
                if (value == null) {
                    throw new IllegalArgumentException("Missing value for path variable: " + paramNames.get(i));
                }
                variables.put(paramNames.get(i), value);
            }
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Path variable extraction failed: " + e.getMessage());
        }

        return variables;
    }

    private Map<String, List<String>> decodeQueryParams(String queryString) {
        Map<String, List<String>> result = new HashMap<>();
        if (queryString == null || queryString.isEmpty()) return result;

        for (String pair : queryString.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = kv[0];
            String value = kv.length > 1 ? kv[1] : "";
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        return result;
    }

    private String extractRequestBody(IHTTPSession session) {
        try {
            int contentLength = 0;

            String contentLengthHeader = session.getHeaders().get("content-length");
            if (contentLengthHeader != null) {
                contentLength = Integer.parseInt(contentLengthHeader);
            }

            if (contentLength > 0) {
                byte[] buffer = session.getInputStream().readNBytes(contentLength);
                return new String(buffer, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Response handle(IHTTPSession session, Matcher pathMatcher) {
        try {
            Map<String, String> pathVariables = extractPathVariables(pathMatcher);
            Map<String, List<String>> queryParams = decodeQueryParams(session.getQueryParameterString());

            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];

            Map<String, String> files = new HashMap<>();
            String rawBody = extractRequestBody(session);

            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                Object argument = null;

                if (parameter.isAnnotationPresent(PathVariable.class)) {
                    String name = parameter.getAnnotation(PathVariable.class).value();
                    String value = pathVariables.get(name);
                    if (value == null) {
                        return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing path variable: " + name);
                    }
                    argument = convertValue(value, parameter.getType(), name);

                } else if (parameter.isAnnotationPresent(RequestParam.class)) {
                    String name = parameter.getAnnotation(RequestParam.class).value();
                    List<String> values = queryParams.get(name);
                    if (values != null && !values.isEmpty()) {
                        argument = convertValue(values.get(0), parameter.getType(), name);
                    } else {
                        argument = null; // treat as optional
                        if (parameter.getType().isPrimitive()) {
                            return NanoResponseHelper.badRequest("Missing required request parameter: " + name);
                        }
                    }

                } else if (parameter.isAnnotationPresent(RequestBody.class)) {
                    if (rawBody == null || rawBody.isEmpty()) {
                        return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing request body.");
                    }
                    try {
                        argument = objectMapper.readValue(rawBody, parameter.getType());
                    } catch (Exception e) {
                        return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Invalid request body.");
                    }
                }

                args[i] = argument;
            }

            Object result = method.invoke(controllerInstance, args);
            String responseJson = objectMapper.writeValueAsString(result);
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json", responseJson);

        } catch (IllegalArgumentException e) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Bad Request: " + e.getMessage());
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getTargetException();
            if (cause instanceof IllegalArgumentException) {
                return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "text/plain", cause.getMessage());
            } else {
                cause.printStackTrace();
                return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/plain", "Internal error: " + cause.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal server error.");
        }
    }

    private NanoHTTPD.Response createJsonResponse(Object result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", json);
        } catch (Exception e) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/plain", "JSON serialization failed");
        }
    }

    private Object convertValue(String raw, Class<?> type, String name) {
        if (raw == null) return null;
        if (type == String.class) return raw;

        if (type == int.class || type == Integer.class) return Integer.parseInt(raw);
        if (type == long.class || type == Long.class) return Long.parseLong(raw);

        if (type == boolean.class || type == Boolean.class) {
            if (!raw.equalsIgnoreCase("true") && !raw.equalsIgnoreCase("false")) {
                throw new IllegalArgumentException("Invalid value for request parameter '"
                        + name + "' - Expected 'true' or 'false'");
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
