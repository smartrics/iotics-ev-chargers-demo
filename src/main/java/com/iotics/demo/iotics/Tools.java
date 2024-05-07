package com.iotics.demo.iotics;

import com.iotics.demo.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import smartrics.iotics.host.IoticsApi;
import smartrics.iotics.host.IoticsApiImpl;
import smartrics.iotics.host.grpc.HostConnection;
import smartrics.iotics.host.grpc.HostConnectionImpl;
import smartrics.iotics.identity.SimpleConfig;
import smartrics.iotics.identity.SimpleIdentityImpl;
import smartrics.iotics.identity.SimpleIdentityManager;
import smartrics.iotics.identity.jna.JnaSdkApiInitialiser;
import smartrics.iotics.identity.jna.OsLibraryPathResolver;
import smartrics.iotics.identity.jna.SdkApi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.Callable;

public final class Tools {
    private static final Logger LOGGER = LogManager.getLogger(Tools.class);

    public static void runSilently(Runnable r) {
        try {
            r.run();
        } catch (Throwable e) {
            LOGGER.warn("exception when running runnable silently: {}", e.getMessage(), e);
        }
    }

    public static Object sf(Callable<Object> r, Object def) {
        try {
            Object ret = r.call();
            if (ret == null) {
                return def;
            }
            return ret;
        } catch (Throwable ignored) {
        }
        return def;
    }

    public static Object sf2(Callable<Object> r, Object def) {
        try {
            Object ret = r.call();
            if (ret == null) {
                return def;
            }
            return ret;
        } catch (Throwable ignored) {
        }
        return def;
    }

    public static boolean getRandomBoolean(double probabilityOfTrue) {
        Random random = new Random();
        if (probabilityOfTrue < 0 || probabilityOfTrue > 1) {
            throw new IllegalArgumentException("Probability must be between 0 and 1");
        }
        return random.nextDouble() < probabilityOfTrue;
    }

    @NotNull
    public static IoticsApi newIoticsApi(SimpleIdentityManager sim, String grpcEndpoint, Duration tokenDuration) throws IOException {
        HostConnection connection = new HostConnectionImpl(grpcEndpoint, sim, tokenDuration);
        return new IoticsApiImpl(connection);
    }

    @NotNull
    public static SimpleIdentityManager newSimpleIdentityManager(Configuration conf, String resolver) throws FileNotFoundException {
        SimpleConfig userConf = SimpleConfig.readConf(Path.of(conf.identity().userIdentityPath()));
        SimpleConfig agentConf = SimpleConfig.readConf(Path.of(conf.identity().agentIdentityPath()));
        OsLibraryPathResolver pathResolver = new OsLibraryPathResolver() {};
        SdkApi api = new JnaSdkApiInitialiser("./lib", pathResolver).get();
        return SimpleIdentityManager.Builder.anIdentityManager()
                .withSimpleIdentity(new SimpleIdentityImpl(api, resolver, userConf.seed(), agentConf.seed()))
                .withAgentKeyID(agentConf.keyId())
                .withUserKeyID(userConf.keyId())
                .withAgentKeyName(agentConf.keyName())
                .withUserKeyName(userConf.keyName())
                .build();
    }


}
