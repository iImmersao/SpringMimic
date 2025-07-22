package com.iimmersao.springmimic.core;

import com.iimmersao.springmimic.annotations.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class ConfigLoader {

    private final Map<String, String> properties = new HashMap<>();

    public ConfigLoader(String profile) {
        loadConfiguration(profile);
    }

    public ConfigLoader() {
        loadConfiguration(null);
    }

    public void loadConfiguration(String profile) {
        String activeProfile = profile;
        if (activeProfile == null) {
            activeProfile = resolveActiveProfile();
        }
        Map<String, Object> config = new LinkedHashMap<>();

        loadConfigFile("application", properties);
        loadConfigFile("application-" + activeProfile, properties);
    }

    private void loadConfigFile(String baseName, Map<String, String> config) {
        loadProperties(baseName + ".properties");
        loadYaml(baseName + ".yml", properties);
        loadYaml(baseName + ".yaml", properties);
    }

    private void loadProperties(String configFileName) {
        // 1. Load framework-level config (from inside the framework JAR)
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(configFileName)) {
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

    private void loadYaml(String fileName, Map<String, String> config) {
        try (InputStream in = getFileStream(fileName)) {
            if (in != null) {
                Yaml yaml = new Yaml();
                Object data = yaml.load(in);
                if (data instanceof Map<?, ?> map) {
                    flattenYaml(map, "", config);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load YAML: " + fileName, e);
        }
    }

    private InputStream getFileStream(String fileName) throws IOException {
        Path path = Paths.get(fileName);
        if (Files.exists(path)) return Files.newInputStream(path);

        return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
    }

    private void flattenYaml(Map<?, ?> source, String prefix, Map<String, String> target) {
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey().toString() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> map) {
                flattenYaml(map, key, target);
            } else {
                target.put(key, value.toString());
            }
        }
    }

    private String resolveActiveProfile() {
        String fromSystem = System.getProperty("spring.profiles.active");
        if (fromSystem != null) return fromSystem;

        String fromEnv = System.getenv("SPRING_PROFILES_ACTIVE");
        if (fromEnv != null) return fromEnv;

        return "default";
    }
}
