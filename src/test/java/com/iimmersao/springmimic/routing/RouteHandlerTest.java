package com.iimmersao.springmimic.routing;

import com.iimmersao.springmimic.annotations.PathVariable;
import com.iimmersao.springmimic.annotations.RequestBody;
import com.iimmersao.springmimic.annotations.RequestParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.iimmersao.springmimic.annotations.GetMapping;
import com.iimmersao.springmimic.annotations.PathVariable;
import com.iimmersao.springmimic.annotations.PostMapping;
import com.iimmersao.springmimic.annotations.RequestBody;
import com.iimmersao.springmimic.annotations.RequestParam;
import com.iimmersao.springmimic.routing.RouteHandler;
import fi.iki.elonen.NanoHTTPD.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class RouteHandlerTest {

    static class User {
        public String id;
        public String name;
    }

    static class TestController {
        @GetMapping("/users/{id}")
        public String getUser(@PathVariable("id") String id, @RequestParam("verbose") boolean verbose) {
            return "User ID: " + id + ", verbose=" + verbose;
        }

        @PostMapping("/users")
        public String createUser(@RequestBody User user) {
            return "Created user: " + user.name;
        }
    }

    // Helper to mock a GET session
    private IHTTPSession mockGetSession(String uri, String queryString) {
        return new IHTTPSession() {
            @Override public NanoHTTPD.Method getMethod() { return NanoHTTPD.Method.GET; }
            @Override public String getUri() { return uri; }
            @Override public Map<String, String> getParms() { return new HashMap<>(); }

            @Override
            public void execute() throws IOException {

            }

            @Override
            public CookieHandler getCookies() {
                return null;
            }

            @Override public Map<String, String> getHeaders() { return new HashMap<>(); }
            @Override public String getQueryParameterString() { return queryString; }
            @Override public Map<String, List<String>> getParameters() { return new HashMap<>(); }
            @Override public InputStream getInputStream() { return null; }
            @Override public void parseBody(Map<String, String> files) { }
            @Override public String getRemoteHostName() { return null; }
            @Override public String getRemoteIpAddress() { return null; }
        };
    }

    // Helper to mock a POST session
    private IHTTPSession mockPostSession(String uri, String bodyJson) {
        return new IHTTPSession() {
            @Override public NanoHTTPD.Method getMethod() { return NanoHTTPD.Method.POST; }
            @Override public String getUri() { return uri; }
            @Override public Map<String, String> getParms() { return new HashMap<>(); }

            @Override
            public void execute() throws IOException {

            }

            @Override
            public CookieHandler getCookies() {
                return null;
            }

            @Override public Map<String, String> getHeaders() { return new HashMap<>(); }
            @Override public String getQueryParameterString() { return bodyJson; } // simplified for test
            @Override public Map<String, List<String>> getParameters() { return new HashMap<>(); }
            @Override public InputStream getInputStream() { return null; }
            @Override public void parseBody(Map<String, String> files) {
                /*
                String[] parts = bodyJson.split("&");
                for (String p : parts) {
                    String[] keyVal = p.split("=");
                    files.put(keyVal[0], keyVal[1]);
                }
                 */
                files.put("postData", bodyJson);
            }
            @Override public String getRemoteHostName() { return null; }
            @Override public String getRemoteIpAddress() { return null; }
        };
    }

    private String readResponseBody(Response response) throws IOException {
        Scanner scanner = new Scanner(response.getData(), "UTF-8").useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    @Test
    void shouldBindPathVariableAndQueryParam() throws Exception {
        TestController controller = new TestController();
        Method method = controller.getClass().getMethod("getUser", String.class, boolean.class);
        RouteHandler handler = new RouteHandler(controller, method, "/users/{id}");

        IHTTPSession session = mockGetSession("/users/abc123", "verbose=true");
        Response response = handler.handle(session);

        assertEquals(Response.Status.OK.getRequestStatus(), response.getStatus().getRequestStatus());
        String body = readResponseBody(response);
        assertEquals("User ID: abc123, verbose=true", body.trim());
    }

    @Test
    void shouldReturnBadRequestForInvalidBooleanParam() throws Exception {
        TestController controller = new TestController();
        Method method = controller.getClass().getMethod("getUser", String.class, boolean.class);
        RouteHandler handler = new RouteHandler(controller, method, "/users/{id}");

        IHTTPSession session = mockGetSession("/users/abc123", "verbose=notabool");
        Response response = handler.handle(session);

        assertEquals(Response.Status.BAD_REQUEST.getRequestStatus(), response.getStatus().getRequestStatus());
    }

    @Test
    void shouldReturnBadRequestForMissingRequiredParam() throws Exception {
        TestController controller = new TestController();
        Method method = controller.getClass().getMethod("getUser", String.class, boolean.class);
        RouteHandler handler = new RouteHandler(controller, method, "/users/{id}");

        IHTTPSession session = mockGetSession("/users/abc123", null);
        Response response = handler.handle(session);

        assertEquals(Response.Status.BAD_REQUEST.getRequestStatus(), response.getStatus().getRequestStatus());
    }

    @Test
    void shouldBindRequestBody() throws Exception {
        TestController controller = new TestController();
        Method method = controller.getClass().getMethod("createUser", User.class);
        RouteHandler handler = new RouteHandler(controller, method, "/users");

        String json = "{\"id\": \"abc123\", \"name\": \"Alice\"}";
        //String json = "id=abc123&name=Alice";
        IHTTPSession session = mockPostSession("/users", json);
        Response response = handler.handle(session);

        assertEquals(Response.Status.OK.getRequestStatus(), response.getStatus().getRequestStatus());
        String body = readResponseBody(response);
        assertTrue(body.contains("Alice"));
    }

    @Test
    void shouldReturnBadRequestForMalformedJson() throws Exception {
        TestController controller = new TestController();
        Method method = controller.getClass().getMethod("createUser", User.class);
        RouteHandler handler = new RouteHandler(controller, method, "/users");

        String malformed = "id=abc123&name";  // malformed
        IHTTPSession session = mockPostSession("/users", malformed);
        Response response = handler.handle(session);

        assertEquals(Response.Status.BAD_REQUEST.getRequestStatus(), response.getStatus().getRequestStatus());
    }
}
