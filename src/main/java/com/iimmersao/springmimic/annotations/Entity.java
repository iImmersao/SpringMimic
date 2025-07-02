package com.iimmersao.springmimic.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Entity {
    //String table() default "";       // For SQL
    //String collection() default "";  // For Mongo
}