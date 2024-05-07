package com.iotics.demo.configuration;

import java.util.Map;

import static com.iotics.demo.configuration.Configuration.checkNotNull;

public record OpenChargeMap(String key, String country, String testDataFile) {

    public OpenChargeMap(Map<String, Object> map) {
        this(checkNotNull(map.get("key")).toString(), checkNotNull(map.get("country")).toString(), checkNotNull(map.get("testDataFile")).toString());
    }

}