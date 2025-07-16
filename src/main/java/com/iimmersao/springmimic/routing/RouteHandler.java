package com.iimmersao.springmimic.routing;

import com.iimmersao.springmimic.annotations.*;
import com.iimmersao.springmimic.core.ApplicationContext;
import com.iimmersao.springmimic.security.Authenticator;
import com.iimmersao.springmimic.security.UserDetails;
import com.iimmersao.springmimic.core.ExceptionHandler;
import com.iimmersao.springmimic.core.util.PathUtils;
import com.iimmersao.springmimic.openapi.MethodParameter;
import com.iimmersao.springmimic.web.PageRequest;
import com.iimmersao.springmimic.web.ResponseFactory;
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

    private final Object controllerInstance;
    private final Method method;
    private final List<MethodParameter> params;
    private final ApplicationContext context;

    private final String routePath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RouteHandler(String routePath, Object controllerInstance, Method method,
                        List<MethodParameter> params, ApplicationContext context) {
        this.controllerInstance = controllerInstance;
        this.method = method;
        this.routePath = routePath;
        this.params = params;
        this.context = context;
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

    public Response handle(IHTTPSession session, Matcher matcher) {
        try {
            // --- 1. Check if authentication is required ---
            boolean requiresAuth = method.isAnnotationPresent(Authenticated.class);
            UserDetails user = null;

            if (requiresAuth) {
                // Get Authorization header
                String authHeader = session.getHeaders().get("authorization");
                if (authHeader == null || !authHeader.startsWith("Basic ")) {
                    return ResponseFactory.unauthorized("Missing or invalid Authorization header");
                }

                // Decode credentials
                String base64Credentials = authHeader.substring("Basic ".length()).trim();
                String decoded = new String(Base64.getDecoder().decode(base64Credentials));
                String[] parts = decoded.split(":", 2);
                if (parts.length != 2) {
                    return ResponseFactory.unauthorized("Invalid Authorization header format");
                }

                String username = parts[0];
                String password = parts[1];

                // Authenticate user
                Authenticator authenticator = context.getBean(Authenticator.class);
                user = authenticator.authenticate(username, password);  // Throws UnauthorizedException if invalid

                // --- 2. Check roles if @RolesAllowed is present ---
                if (method.isAnnotationPresent(RolesAllowed.class)) {
                    RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
                    List<String> requiredRoles = Arrays.asList(rolesAllowed.value());

                    boolean hasRole = requiredRoles.stream().anyMatch(user.getRoles()::contains);
                    if (!hasRole) {
                        return ResponseFactory.forbidden("Forbidden - User lacks required role(s)");
                    }
                }
            }

            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];

            // Extract path variables
            Map<String, String> pathVariables = extractPathVariables(matcher);

            // Extract query params (as Map<String, List<String>>)
            String queryString = session.getQueryParameterString();
            Map<String, List<String>> queryParams = decodeQueryParams(queryString);

            // Extract request body if needed
            String rawBody = null;
            String httpMethod = session.getMethod().name();
            if (httpMethod.equals("POST") || httpMethod.equals("PUT") || httpMethod.equals("PATCH")) {
                String contentLengthHeader = session.getHeaders().get("content-length");
                if (contentLengthHeader != null) {
                    int contentLength = Integer.parseInt(contentLengthHeader);
                    rawBody = new String(session.getInputStream().readNBytes(contentLength), StandardCharsets.UTF_8);
                }
            }

            // Resolve arguments
            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];

                if (param.getType().equals(PageRequest.class)) {
                    PageRequest pageRequest = new PageRequest();

                    List<String> pageVals = queryParams.get("page");
                    List<String> sizeVals = queryParams.get("size");
                    List<String> sortVals = queryParams.get("sort");

                    if (pageVals != null && !pageVals.isEmpty()) {
                        try {
                            pageRequest.setPage(Integer.parseInt(pageVals.getFirst()));
                        } catch (NumberFormatException ignored) {}
                    }

                    if (sizeVals != null && !sizeVals.isEmpty()) {
                        try {
                            pageRequest.setSize(Integer.parseInt(sizeVals.getFirst()));
                        } catch (NumberFormatException ignored) {}
                    }

                    if (sortVals != null && !sortVals.isEmpty()) {
                        pageRequest.setSortBy(sortVals.getFirst());
                    }

                    args[i] = pageRequest;
                    continue;
                }

                if (param.isAnnotationPresent(PathVariable.class)) {
                    String name = param.getAnnotation(PathVariable.class).value();
                    String value = pathVariables.get(name);
                    if (value == null) {
                        throw new IllegalArgumentException("Missing path variable: " + name);
                    }
                    args[i] = convertValue(value, param.getType(), name);

                } else if (param.isAnnotationPresent(RequestParam.class)) {
                    String name = param.getAnnotation(RequestParam.class).value();
                    List<String> values = queryParams.get(name);

                    if (values != null && !values.isEmpty()) {
                        args[i] = convertValue(values.getFirst(), param.getType(), name);
                    } else {
                        args[i] = null; // treat as optional
                        if (param.getType().isPrimitive()) {
                            throw new IllegalArgumentException("Missing required request parameter: " + name);
                        }
                    }

                } else if (param.isAnnotationPresent(RequestBody.class)) {
                    if (rawBody == null || rawBody.isBlank()) {
                        throw new IllegalArgumentException("Missing request body");
                    }
                    args[i] = objectMapper.readValue(rawBody, param.getType());

                } else if (param.getType().equals(NanoHTTPD.IHTTPSession.class)) {
                        args[i] = session;
                } else {
                    args[i] = null;
                }
            }

            // Invoke and serialize result
            Object result = method.invoke(controllerInstance, args);

            if (method.isAnnotationPresent(ResponseBody.class)) {
                String body;
                if (result == null) {
                    body = "";
                } else if (result instanceof String) {
                    body = (String) result;
                } else {
                    body = objectMapper.writeValueAsString(result); // JSON
                }

                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json", body);
            } else {
                // Check for @Produces on the method
                Produces produces = method.getAnnotation(Produces.class);

                if (produces != null && "application/json".equalsIgnoreCase(produces.value())) {
                    String json = new ObjectMapper().writeValueAsString(result);
                    return NanoHTTPD.newFixedLengthResponse(
                            NanoHTTPD.Response.Status.OK,
                            "application/json",
                            json
                    );
                }

                String json = objectMapper.writeValueAsString(result);
                return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", json);
            }

        } catch (InvocationTargetException e) {
            return ExceptionHandler.handle((Exception) e.getTargetException());
        } catch (Exception e) {
            return ExceptionHandler.handle(e);
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

    public List<MethodParameter> getMethodParameters() {
        return params;
    }
}
