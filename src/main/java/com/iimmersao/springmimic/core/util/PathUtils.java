package com.iimmersao.springmimic.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathUtils {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([^/]+?)}");

    public static List<String> extractPathParamNames(String pathTemplate) {
        List<String> paramNames = new ArrayList<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(pathTemplate);
        while (matcher.find()) {
            paramNames.add(matcher.group(1));
        }
        return paramNames;
    }
}