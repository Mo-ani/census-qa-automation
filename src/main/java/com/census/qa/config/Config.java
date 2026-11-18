package com.census.qa.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Properties;

/**
 * Single access point for environment settings.
 *
 * <p>Lookup order for a key such as {@code base.url}:
 * <ol>
 *   <li>JVM system property ({@code -Dbase.url=...})</li>
 *   <li>environment variable ({@code BASE_URL})</li>
 *   <li>{@code configuration.properties} on the classpath</li>
 * </ol>
 * so the same suite runs unchanged on a laptop and in a CI pipeline.
 */
public final class Config {

    private static final String FILE = "configuration.properties";
    private static final Properties FILE_VALUES = load();

    private Config() {
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream(FILE)) {
            if (in == null) {
                throw new IllegalStateException(FILE + " was not found on the classpath");
            }
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + FILE, e);
        }
        return properties;
    }

    /** Returns the value for {@code key}, failing fast with a clear message when it is missing. */
    public static String get(String key) {
        String value = getOrDefault(key, null);
        if (value == null) {
            throw new IllegalStateException("Missing configuration value: '" + key + "' (see " + FILE + ")");
        }
        return value;
    }

    public static String getOrDefault(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (isBlank(value)) {
            value = System.getenv(key.toUpperCase().replace('.', '_'));
        }
        if (isBlank(value)) {
            value = FILE_VALUES.getProperty(key);
        }
        return isBlank(value) ? defaultValue : value.trim();
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    // --- Typed shortcuts for the values used all over the framework ---------

    public static String baseUrl() {
        return get("base.url").replaceAll("/+$", "");
    }

    public static Duration explicitWait() {
        return Duration.ofSeconds(getInt("explicit.wait.seconds"));
    }

    /** Time zone of the application server; needed to read the stored date of birth as a date. */
    public static ZoneId appTimeZone() {
        return ZoneId.of(get("app.timezone"));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
