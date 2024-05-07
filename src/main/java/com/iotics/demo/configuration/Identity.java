package com.iotics.demo.configuration;

import java.time.Duration;
import java.util.Map;

import static com.iotics.demo.configuration.Configuration.checkNotNull;

public record Identity(String userIdentityPath, String agentIdentityPath, String twinIdentityPath, Duration tokenDuration) {

    public Identity(Map<String, Object> map) {
        this(checkNotNull(map.get("userIdentityPath")).toString()
                , checkNotNull(map.get("agentIdentityPath")).toString()
                , checkNotNull(map.get("twinIdentityPath")).toString()
                , Duration.parse(checkNotNull(map.get("tokenDuration")).toString())
        );
    }

}
