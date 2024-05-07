package com.iotics.demo.configuration;

import java.util.Map;

import static com.iotics.demo.configuration.Configuration.checkNotNull;

public record Space(String name) {
    public Space(Map<String, Object> map) {
        this(checkNotNull(map.get("name")).toString());
    }
}
