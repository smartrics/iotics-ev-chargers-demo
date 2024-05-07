package com.iotics.demo.configuration;

import java.time.Duration;
import java.util.Map;

import static com.iotics.demo.configuration.Configuration.checkNotNull;

public record Connector(Integer makerThreadPoolSize, Duration sharePeriod, Boolean forceCreation, Boolean forceDeletion) {
    public Connector(Map<String, Object> map) {
        this(Integer.parseInt(checkNotNull(map.get("makerThreadPoolSize")).toString())
                , Duration.parse(checkNotNull(map.get("sharePeriod")).toString())
                , Boolean.parseBoolean(checkNotNull(map.get("forceCreation")).toString())
                , Boolean.parseBoolean(checkNotNull(map.get("forceDeletion")).toString())
                );
    }
}
