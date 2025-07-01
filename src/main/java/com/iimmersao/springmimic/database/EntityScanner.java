package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.annotations.Entity;
import io.github.classgraph.ClassGraph;

import java.util.HashSet;
import java.util.Set;

public class EntityScanner {

    public Set<Class<?>> scanEntities(String... basePackages) {
        Set<Class<?>> result = new HashSet<>();

        try (var scanResult = new ClassGraph()
                .enableClassInfo()
                .acceptPackages(basePackages)
                .enableAnnotationInfo()
                .scan()) {

            scanResult.getClassesWithAnnotation(Entity.class.getName())
                    .forEach(classInfo -> result.add(classInfo.loadClass()));
        }

        return result;
    }
}
