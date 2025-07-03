package com.iimmersao.springmimic.core;

import com.iimmersao.springmimic.annotations.Component;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Component
public class ConfigLoader {

    private final Map<String, String> config = new HashMap<>();

    public ConfigLoader(String filename) throws IOException {
        load(filename);
    }

    private void load(String filename) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (input == null) {
                throw new FileNotFoundException("Configuration file not found: " + filename);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and full-line comments
                if (line.isEmpty() || line.startsWith("#")) continue;

                // Remove inline comments
                int commentIndex = line.indexOf("#");
                if (commentIndex >= 0) {
                    line = line.substring(0, commentIndex).trim();
                }

                // Parse key=value
                int equalsIndex = line.indexOf("=");
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    config.put(key, value);
                }
            }
        }
    }

    public String get(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    public String get(String key) {
        return config.get(key);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(config.get(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
