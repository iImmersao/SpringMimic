package com.iimmersao.springmimic.routing;

import com.iimmersao.springmimic.annotations.PathVariable;
import com.iimmersao.springmimic.annotations.RequestBody;
import com.iimmersao.springmimic.annotations.RequestParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RouteHandlerTest {

    static class TestController {
        public String echo(@PathVariable("id") String id,
                           @RequestParam("tag") String tag,
                           @RequestBody TestData body) {
            return "ID=" + id + ", tag=" + tag + ", name=" + body.name;
        }
    }

    static class TestData {
        public String name;
    }

    @Test
    void testHandleBindsAndInvokesCorrectly() throws Exception {
        // Arrange
        TestController controller = new TestController();
        Method method = TestController.class.getMethod("echo", String.class, String.class, TestData.class);

        Map<String, String> pathVars = Map.of("id", "123");
        RouteHandler handler = new RouteHandler(controller, method, pathVars);

        IHTTPSession session = mock(IHTTPSession.class);
        Map<String, String> queryParams = Map.of("tag", "alpha");
        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("postData", "{\"name\":\"Philip\"}");

        when(session.getParms()).thenReturn(queryParams);
        doAnswer(invocation -> {
            Map<String, String> map = invocation.getArgument(0);
            map.putAll(bodyMap);
            return null;
        }).when(session).parseBody(any());

        // Act
        Object result = handler.handle(session);

        // Assert
        assertEquals("ID=123, tag=alpha, name=Philip", result);
    }
}
