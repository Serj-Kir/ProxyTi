package proxyti.api;

import java.util.Properties;

/** Parsed {@code plugin.properties} descriptor. */
public record PluginDescription(String name, String version, String author, String main, String apiVersion) {
    public static PluginDescription from(Properties properties, String fallbackName) {
        String name = properties.getProperty("name", fallbackName).trim();
        String main = properties.getProperty("main", "").trim();
        return new PluginDescription(
                name,
                properties.getProperty("version", "unknown").trim(),
                properties.getProperty("author", "unknown").trim(),
                main,
                properties.getProperty("api", "1.0").trim());
    }
}