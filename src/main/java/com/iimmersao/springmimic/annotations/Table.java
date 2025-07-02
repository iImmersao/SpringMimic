package com.iimmersao.springmimic.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
    String name() default "";
}
