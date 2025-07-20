package com.iimmersao.springmimic.routing;

import com.iimmersao.springmimic.annotations.Component;
import com.iimmersao.springmimic.annotations.ResponseBody;
import com.iimmersao.springmimic.annotations.RestController;
import com.iimmersao.springmimic.core.ApplicationContext;
import com.iimmersao.springmimic.openapi.MethodParameter;

import java.lang.reflect.Method;
import java.util.List;

@Component
public class RouteHandlerFactory {

    private final ApplicationContext context;

    public RouteHandlerFactory(ApplicationContext context) {
        this.context = context;
    }

    public RouteHandler create(String method,
                               String path,
                               Object handlerInstance,
                               Method handlerMethod,
                               List<MethodParameter> parameters) {
        boolean responseBodyPresent =
                handlerMethod.isAnnotationPresent(ResponseBody.class)
                        || handlerMethod.getDeclaringClass().isAnnotationPresent(ResponseBody.class)
                        || handlerMethod.getDeclaringClass().isAnnotationPresent(RestController.class);

        return new RouteHandler(path, handlerInstance, handlerMethod, parameters, context, responseBodyPresent);
    }
}
