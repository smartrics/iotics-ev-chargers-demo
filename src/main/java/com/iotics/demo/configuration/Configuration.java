package com.iotics.demo.configuration;

import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

public record Configuration(Space space, OpenChargeMap openChargeMap, Identity identity, Connector connector) {

    public static Object checkNotNull(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("null value");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> castToMap(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("null value");
        }
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("invalid configuration value, should be a map");
        }
        return (Map<String, Object>) value;
    }

    public static Configuration loadConfiguration(String filePath) throws IOException {
        Map<String, Object> map = loadConfigurationMap(filePath);
        return toConfiguration(map);
    }

    public static Map<String, Object> loadConfigurationMap(String filePath) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> customConfig = yaml.load(new FileReader(filePath));

        if (customConfig.containsKey("import")) {
            String sharedConfigFilePath = (String) customConfig.get("import");
            Map<String, Object> sharedConfig = loadConfigurationMap(sharedConfigFilePath);
            customConfig.remove("import"); // Remove the import key from the custom config
            customConfig.putAll(sharedConfig); // Merge shared config into custom config
        }
        return customConfig;
    }

    private static Configuration toConfiguration(Map<String, Object> map) throws IOException {
        Space space = new Space(castToMap(map.get("space")));
        OpenChargeMap openChargeMap = new OpenChargeMap(castToMap(map.get("openChargeMap")));
        Identity identity = new Identity(castToMap(map.get("identity")));
        Connector connector = new Connector(castToMap(map.get("connector")));
        return new Configuration(space, openChargeMap, identity, connector);
    }
}
