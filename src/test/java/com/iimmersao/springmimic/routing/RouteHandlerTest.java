package com.iimmersao.springmimic.routing;

import com.iimmersao.springmimic.annotations.GetMapping;
import com.iimmersao.springmimic.annotations.PathVariable;
import com.iimmersao.springmimic.annotations.RequestBody;
import com.iimmersao.springmimic.annotations.RequestParam;
import fi.iki.elonen.NanoHTTPD;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.lang.reflect.Field;

import static fi.iki.elonen.NanoHTTPD.Response;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RouteHandlerTest {

    public static class TestController {
        public String hello(String name) {
            return "Hello, " + name;
        }

        public String createUser(@RequestBody User user) {
            return "Created user: " + user.name;
        }

        public String updateUser(@RequestBody User user) {
            return "Updated user: " + user.name;
        }

        public String patchUser(@RequestBody User user) {
            return "Patched user: " + user.name;
        }

        public String deleteUser(@PathVariable("id") String id) {
            return "Deleted user with ID: " + id;
        }

        @GetMapping("/users/{id}")
        public String getUser(@PathVariable("id") String id, @RequestParam("verbose") boolean verbose) {
            return "User ID: " + id + ", verbose=" + verbose;
        }

        public static class User {
            public String id;
            public String name;
        }
    }

    private String extractResponseBody(NanoHTTPD.Response response) throws Exception {
        Field dataField = NanoHTTPD.Response.class.getDeclaredField("data");
        dataField.setAccessible(true);
        InputStream dataStream = (InputStream) dataField.get(response);
        return new String(dataStream.readAllBytes(), StandardCharsets.UTF_8);
    }


    private NanoHTTPD.IHTTPSession createMockSession(String method, String uri, String bodyJson, String queryString) throws Exception {
        NanoHTTPD.IHTTPSession session = mock(NanoHTTPD.IHTTPSession.class);
        when(session.getMethod()).thenReturn(NanoHTTPD.Method.valueOf(method));
        when(session.getUri()).thenReturn(uri);
        when(session.getQueryParameterString()).thenReturn(queryString);

        if (queryString != null) {
            Map<String, String> queryParams = new HashMap<>();
            for (String param : queryString.split("&")) {
                String[] parts = param.split("=");
                if (parts.length == 2) {
                    queryParams.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                            URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                }
            }
            when(session.getParms()).thenReturn(queryParams);
        } else {
            when(session.getParms()).thenReturn(Collections.emptyMap());
        }

        when(session.getHeaders()).thenReturn(
                bodyJson != null ? Map.of("content-length", String.valueOf(bodyJson.length())) : Map.of()
        );

        InputStream inputStream = bodyJson != null
                ? new ByteArrayInputStream(bodyJson.getBytes(StandardCharsets.UTF_8))
                : new ByteArrayInputStream(new byte[0]);
        when(session.getInputStream()).thenReturn(inputStream);

        return session;
    }

    private Matcher matchUri(String routeTemplate, String uri) {
        String regex = routeTemplate.replaceAll("\\{[^/]+?\\}", "([^/]+)");
        Pattern pattern = Pattern.compile("^" + regex + "$", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(uri);
    }

    @Test
    void shouldBindPathVariableAndQueryParam() throws Exception {
        NanoHTTPD.IHTTPSession session = createMockSession("GET", "/users/abc123", null,"verbose=true");

        TestController controller = new TestController();
        Method method = controller.getClass().getMethod("getUser", String.class, boolean.class);
        RouteHandler handler = new RouteHandler("GET", "/users/{id}", controller, method);

        Matcher matcher = matchUri("/users/{id}", "/users/abc123");
        Response response = handler.handle(session, matcher);

        assertEquals(Response.Status.OK.getRequestStatus(), response.getStatus().getRequestStatus());
        String body = extractResponseBody(response);
        assertEquals("\"User ID: abc123, verbose=true\"", body.trim());
    }

    @Test
    void shouldReturnBadRequestForInvalidBooleanParam() throws Exception {
        NanoHTTPD.IHTTPSession session = createMockSession("GET","/users/abc123", null, "verbose=notabool");

        TestController controller = new TestController();
        Method method = controller.getClass().getMethod("getUser", String.class, boolean.class);
        RouteHandler handler = new RouteHandler("GET", "/users/{id}", controller, method);

        Matcher matcher = matchUri("/users/{id}", "/users/abc123");
        Response response = handler.handle(session, matcher);

        assertEquals(Response.Status.BAD_REQUEST.getRequestStatus(), response.getStatus().getRequestStatus());
    }

    @Test
    void shouldReturnBadRequestForMissingRequiredParam() throws Exception {
        NanoHTTPD.IHTTPSession session = createMockSession("GET", "/users/abc123", null, null);

        TestController controller = new TestController();
        Method method = controller.getClass().getMethod("getUser", String.class, boolean.class);
        RouteHandler handler = new RouteHandler("GET", "/users/{id}", controller, method);

        Matcher matcher = matchUri("/users/{id}", "/users/abc123");
        Response response = handler.handle(session, matcher);

        assertEquals(Response.Status.BAD_REQUEST.getRequestStatus(), response.getStatus().getRequestStatus());
    }

    @Test
    void shouldBindRequestBody() throws Exception {
        String json = "{\"id\": \"abc123\", \"name\": \"Alice\"}";
        NanoHTTPD.IHTTPSession session = createMockSession("POST", "/users", json, null);

        TestController controller = new TestController();
        Method method = controller.getClass().getMethod("createUser", TestController.User.class);
        RouteHandler handler = new RouteHandler("POST", "/users", controller, method);

        Matcher matcher = matchUri("/users", "/users");
        Response response = handler.handle(session, matcher);

        assertEquals(Response.Status.OK.getRequestStatus(), response.getStatus().getRequestStatus());
        String body = extractResponseBody(response);
        assertTrue(body.contains("Alice"));
    }

    @Test
    void shouldReturnBadRequestForMalformedJson() throws Exception {
        String malformed = "id=abc123&name";  // malformed
        NanoHTTPD.IHTTPSession session = createMockSession("POST", "/users", malformed, null);

        TestController controller = new TestController();
        Method method = controller.getClass().getMethod("createUser", TestController.User.class);
        RouteHandler handler = new RouteHandler("POST", "/users", controller, method);

        Matcher matcher = matchUri("/users", "/users");
        assertTrue(matcher.matches());

        Response response = handler.handle(session, matcher);

        assertEquals(Response.Status.BAD_REQUEST.getRequestStatus(), response.getStatus().getRequestStatus());
    }

    @Test
    public void shouldHandlePostRequestBody() throws Exception {
        String bodyJson = "{\"name\":\"John\"}";
        NanoHTTPD.IHTTPSession session = createMockSession("POST", "/users", bodyJson, null);

        TestController controller = new TestController();
        Method method = TestController.class.getMethod("createUser", TestController.User.class);
        RouteHandler handler = new RouteHandler("POST", "/users", controller, method);

        Matcher matcher = matchUri("/users", "/users");
        assertTrue(matcher.matches());

        Response response = handler.handle(session, matcher);
        assertEquals(Response.Status.OK, response.getStatus());
        String body = extractResponseBody(response);
        assertTrue(body.contains("Created user: John"));
    }

    @Test
    public void shouldHandlePutRequestBody() throws Exception {
        String bodyJson = "{\"name\":\"Jane\"}";
        NanoHTTPD.IHTTPSession session = createMockSession("PUT", "/users", bodyJson, null);

        TestController controller = new TestController();
        Method method = TestController.class.getMethod("updateUser", TestController.User.class);
        RouteHandler handler = new RouteHandler("PUT", "/users", controller, method);

        Matcher matcher = matchUri("/users", "/users");
        assertTrue(matcher.matches());

        Response response = handler.handle(session, matcher);
        assertEquals(Response.Status.OK, response.getStatus());
        String body = extractResponseBody(response);
        assertTrue(body.contains("Updated user: Jane"));
    }

    @Test
    public void shouldHandlePatchRequestBody() throws Exception {
        String bodyJson = "{\"name\":\"Mike\"}";
        NanoHTTPD.IHTTPSession session = createMockSession("PATCH", "/users", bodyJson, null);

        TestController controller = new TestController();
        Method method = TestController.class.getMethod("patchUser", TestController.User.class);
        RouteHandler handler = new RouteHandler("PATCH", "/users", controller, method);

        Matcher matcher = matchUri("/users", "/users");
        assertTrue(matcher.matches());

        Response response = handler.handle(session, matcher);
        assertEquals(Response.Status.OK, response.getStatus());
        String body = extractResponseBody(response);
        assertTrue(body.contains("Patched user: Mike"));
    }

    @Test
    public void shouldHandleDeleteWithPathVariable() throws Exception {
        NanoHTTPD.IHTTPSession session = createMockSession("DELETE", "/users/abc123", "", null);

        TestController controller = new TestController();
        Method method = TestController.class.getMethod("deleteUser", String.class);
        RouteHandler handler = new RouteHandler("DELETE", "/users/{id}", controller, method);

        Matcher matcher = matchUri("/users/{id}", "/users/abc123");
        assertTrue(matcher.matches());

        Response response = handler.handle(session, matcher);
        assertEquals(Response.Status.OK, response.getStatus());
        String body = extractResponseBody(response);
        assertTrue(body.contains("Deleted user with ID: abc123"));
    }
}
