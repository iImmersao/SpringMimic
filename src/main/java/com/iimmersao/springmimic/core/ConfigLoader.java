package com.iimmersao.springmimic.core;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/*
public class ConfigLoader {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                System.out.println("Loading properties");
                properties.load(input);
            } else {
                System.err.println("No application.properties found in classpath.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                System.err.printf("Invalid int for config key '%s': %s%n", key, value);
            }
        }
        return defaultValue;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
}
*/


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
