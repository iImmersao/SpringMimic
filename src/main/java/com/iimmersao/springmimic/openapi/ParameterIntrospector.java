package com.iimmersao.springmimic.openapi;

import com.iimmersao.springmimic.annotations.PathVariable;
import com.iimmersao.springmimic.annotations.RequestBody;
import com.iimmersao.springmimic.annotations.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class ParameterIntrospector {
    public static List<MethodParameter> extractParameters(Method method) {
        List<MethodParameter> params = new ArrayList<>();

        Parameter[] javaParams = method.getParameters();  // java.lang.reflect.Parameter
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        Class<?>[] paramTypes = method.getParameterTypes();

        for (int i = 0; i < javaParams.length; i++) {
            String paramName = javaParams[i].getName(); // Ensure compiled with `-parameters` to keep names
            Class<?> paramType = paramTypes[i];
            Annotation[] annotations = paramAnnotations[i];
            RequestParam req = javaParams[i].getAnnotation(RequestParam.class);
            if (req != null && !req.value().isEmpty()) {
                paramName = req.value();
            }
            PathVariable pathVar = javaParams[i].getAnnotation(PathVariable.class);
            if (pathVar != null && !pathVar.value().isEmpty()) {
                paramName = pathVar.value();
            }
            boolean required = true;

            for (Annotation a : annotations) {
                if (a instanceof RequestParam) {
                    required = ((RequestParam) a).required();
                } else if (a instanceof RequestBody || a instanceof PathVariable) {
                    required = true;
                }
            }

            params.add(new MethodParameter(paramName, paramType, annotations, required));
        }

        return params;
    }
}
