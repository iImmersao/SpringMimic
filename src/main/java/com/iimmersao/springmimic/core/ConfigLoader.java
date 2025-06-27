package com.iimmersao.springmimic.core;

/*
import java.io.*;
import java.util.*;

public class ConfigLoader {

    private final Properties properties = new Properties();

    public ConfigLoader(String filename) {
        try (InputStream input = new FileInputStream(filename)) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file: " + filename, e);
        }
    }

    public String get(String key) {
        return properties.getProperty(key);
    }
}
 */


import java.io.*;
import java.util.Properties;

public class ConfigLoader {
    private final Properties properties = new Properties();

    public ConfigLoader(String filename) {
        try {
            InputStream input = tryLoad(filename);
            if (input == null) {
                throw new FileNotFoundException("Config not found: " + filename);
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config: " + filename, e);
        }
    }

    private InputStream tryLoad(String filename) throws FileNotFoundException {
        // Try filesystem first
        File file = new File(filename);
        if (file.exists()) {
            System.out.println("Loading config from file system: " + file.getAbsolutePath());
            return new FileInputStream(file);
        }

        // Try classpath
        InputStream resource = getClass().getClassLoader().getResourceAsStream(filename);
        if (resource != null) {
            System.out.println("Loading config from classpath: " + filename);
        }
        return resource;
    }

    public String get(String key) {
        return properties.getProperty(key);
    }
}
