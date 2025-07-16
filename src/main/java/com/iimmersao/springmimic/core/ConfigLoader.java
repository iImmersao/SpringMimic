package com.iimmersao.springmimic.core;

import com.iimmersao.springmimic.annotations.Component;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@Component
public class ConfigLoader {

    private final Map<String, String> properties = new HashMap<>();
    private String configFileName = "application.properties";

    public ConfigLoader() {
        loadProperties();
    }

    public ConfigLoader(String filename) throws IOException {
        this.configFileName = filename;
        loadProperties();
    }

    private void loadProperties() {
        // 1. Load framework-level config (from inside the framework JAR)
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                loadFromStream(in);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load framework configuration", e);
        }

        // 2. Load application-level config (from application JAR or classes)
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(configFileName)) {
            if (in != null) {
                loadFromStream(in); // Overrides framework keys
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application configuration", e);
        }
    }

    private void loadFromStream(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = stripInlineComment(line.trim());
                if (line.isEmpty() || line.startsWith("#")) continue;

                int idx = line.indexOf('=');
                if (idx != -1) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    properties.put(key, value);
                }
            }
        }
    }

    private String stripInlineComment(String line) {
        int commentStart = line.indexOf('#');
        return (commentStart >= 0) ? line.substring(0, commentStart).trim() : line;
    }

    public String get(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }

    public String get(String key) {
        return properties.get(key);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.get(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public Map<String, String> getSubProperties(String prefix, Set<String> allowedKeys) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(prefix)) {
                String subKey = key.substring(prefix.length());
                if (allowedKeys == null || allowedKeys.contains(subKey)) {
                    result.put(subKey, entry.getValue());
                }
            }
        }
        return result;
    }
}
