package com.iimmersao.springmimic.core;

import com.iimmersao.springmimic.annotations.Component;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
    /*
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
*/

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
}
