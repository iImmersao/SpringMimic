package com.iimmersao.springmimic.openapi;

import com.iimmersao.springmimic.annotations.PathVariable;
import com.iimmersao.springmimic.annotations.RequestBody;
import com.iimmersao.springmimic.annotations.RequestParam;

import java.lang.annotation.Annotation;
import java.util.Arrays;

public class MethodParameter {
    private final String name;
    private final Class<?> type;
    private final Annotation[] annotations;
    private final boolean required;

    public MethodParameter(String name, Class<?> type, Annotation[] annotations, boolean required) {
        this.name = name;
        this.type = type;
        this.annotations = annotations;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
        return Arrays.stream(annotations).anyMatch(a -> a.annotationType().equals(annotationType));
    }

    public boolean isRequestBody() {
        return Arrays.stream(annotations).anyMatch(a -> a.annotationType() == RequestBody.class);
    }

    public boolean isRequestParam() {
        return Arrays.stream(annotations).anyMatch(a -> a.annotationType() == RequestParam.class);
    }

    public boolean isPathVariable() {
        return Arrays.stream(annotations).anyMatch(a -> a.annotationType() == PathVariable.class);
    }
}
