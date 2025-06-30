package com.iimmersao.springmimic.annotations;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PatchMapping {
    String value();
}
