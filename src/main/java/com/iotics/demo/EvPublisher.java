package com.iotics.demo;

import com.iotics.demo.configuration.Configuration;
import com.iotics.demo.iotics.EvChargerTwin;
import com.iotics.demo.iotics.EvConnectionTwin;
import com.iotics.demo.ocm.Connection;
import com.iotics.demo.ocm.EvCharger;
import com.iotics.demo.ocm.OcmApi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import smartrics.iotics.host.HostEndpoints;
import smartrics.iotics.host.HttpServiceRegistry;
import smartrics.iotics.host.IoticsApi;
import smartrics.iotics.identity.SimpleIdentityManager;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.iotics.demo.iotics.Tools.newIoticsApi;
import static com.iotics.demo.iotics.Tools.newSimpleIdentityManager;

/**
 * Sample publisher application that publishes data on some of the twins created via maker.
 */
public class EvPublisher {
    private static final Logger LOGGER = LogManager.getLogger(EvPublisher.class);

    public static void main(String[] args) throws Exception {
        Configuration conf = Configuration.loadConfiguration("src/main/resources/publisher.yaml");

        HttpServiceRegistry sr = new HttpServiceRegistry(conf.space().name());
        HostEndpoints endpoints = sr.find();

        SimpleIdentityManager sim = newSimpleIdentityManager(conf, endpoints.resolver());
        IoticsApi ioticsApi = newIoticsApi(sim, endpoints.grpc(), conf.identity().tokenDuration());

        List<EvCharger> foundChargers = OcmApi.loadEvChargersFromPath(conf.openChargeMap().testDataFile());
        ScheduledExecutorService apiExecutor = Executors.newScheduledThreadPool(conf.connector().makerThreadPoolSize());
        Duration period = conf.connector().sharePeriod();
        foundChargers.forEach(evCharger -> {
            EvChargerTwin evChargerTwin = shareEvChargerTwin(evCharger, period, sim, ioticsApi, apiExecutor);
            Arrays.stream(evCharger.connections()).toList().forEach(connection -> shareConnectionTwin(evChargerTwin, connection, period, sim, ioticsApi, apiExecutor));
        });
    }

    private static void shareConnectionTwin(EvChargerTwin evChargerTwin,  Connection connection, Duration period, SimpleIdentityManager sim, IoticsApi ioticsApi, ScheduledExecutorService apiExecutor) {
        EvConnectionTwin twin = new EvConnectionTwin(connection, evChargerTwin, sim, ioticsApi);
        apiExecutor.scheduleAtFixedRate(() -> {
            LOGGER.info("Sharing Connection data [did={}, payload='{}']", twin.getMyIdentity().did(), twin.getShareFeedDataRequest());
            twin.share();
        }, 0, period.toSeconds(), TimeUnit.SECONDS);
    }

    private static EvChargerTwin shareEvChargerTwin(EvCharger evCharger, Duration period, SimpleIdentityManager sim, IoticsApi ioticsApi, ScheduledExecutorService apiExecutor) {
        EvChargerTwin twin = new EvChargerTwin(evCharger, sim, ioticsApi);
        apiExecutor.scheduleAtFixedRate(() -> {
            LOGGER.info("Sharing EvCharger data [did={}, payload='{}']", twin.getMyIdentity().did(), twin.getShareFeedDataRequest());
            twin.share();
        }, 0, period.toSeconds(), TimeUnit.SECONDS);
        return twin;
    }
}