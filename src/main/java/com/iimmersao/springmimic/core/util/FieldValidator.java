package com.iimmersao.springmimic.core.util;

import java.util.ArrayList;
import java.util.List;

public class FieldValidator {

    public static void validateFieldsExist(Class<?> clazz, List<String> fields) {
        List<String> missing = new ArrayList<>();
        for (String field : fields) {
            try {
                clazz.getDeclaredField(field);
            } catch (NoSuchFieldException e) {
                missing.add(field);
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Invalid sort field(s): " + String.join(", ", missing));
        }
    }
}
