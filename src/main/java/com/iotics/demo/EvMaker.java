package com.iotics.demo;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.iotics.api.*;
import com.iotics.demo.configuration.Configuration;
import com.iotics.demo.iotics.EvChargerTwin;
import com.iotics.demo.iotics.EvConnectionTwin;
import com.iotics.demo.ocm.Connection;
import com.iotics.demo.ocm.EvCharger;
import com.iotics.demo.ocm.OcmApi;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import dev.failsafe.RetryPolicyBuilder;
import dev.failsafe.function.CheckedSupplier;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import smartrics.iotics.connectors.twins.AbstractTwin;
import smartrics.iotics.host.Builders;
import smartrics.iotics.host.HostEndpoints;
import smartrics.iotics.host.HttpServiceRegistry;
import smartrics.iotics.host.IoticsApi;
import smartrics.iotics.identity.SimpleIdentityManager;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.iotics.demo.iotics.Tools.newIoticsApi;
import static com.iotics.demo.iotics.Tools.newSimpleIdentityManager;
import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * Makes the digital twins based on the data on OpenChargersMap.
 *
 * You're expected to run this once to create the twins, then use the publisher subscriber and analytics apps to use them.
 */
public class EvMaker {
    private static final Logger LOGGER = LogManager.getLogger(EvMaker.class);

    private static final AtomicInteger twinsMade = new AtomicInteger(0);
    private static final AtomicInteger twinsToBuild = new AtomicInteger(0);

    private record MakeResult(String did, Optional<CompletableFuture<UpsertTwinResponse>> maybeFuture, AbstractTwin twin){}

    public static void main(String[] args) throws Exception {
        Configuration conf = Configuration.loadConfiguration("src/main/resources/shared.yaml");
        HttpServiceRegistry sr = new HttpServiceRegistry(conf.space().name());
        HostEndpoints endpoints = sr.find();

        SimpleIdentityManager sim = newSimpleIdentityManager(conf, endpoints.resolver());
        IoticsApi ioticsApi = newIoticsApi(sim, endpoints.grpc(), conf.identity().tokenDuration());

        Map<String, String> listedTwins = loadKnownTwins(sim, ioticsApi);
        List<EvCharger> foundChargers = OcmApi.loadEvChargersFromPath(conf.openChargeMap().testDataFile());
        ExecutorService apiExecutor = newFixedThreadPool(conf.connector().makerThreadPoolSize());
        foundChargers.forEach(evCharger -> {
            twinsToBuild.incrementAndGet();
            Arrays.stream(evCharger.connections()).toList().forEach(connection -> twinsToBuild.incrementAndGet());
        });

        if(conf.connector().forceDeletion()) {
            deleteAll(foundChargers, sim, ioticsApi, apiExecutor, listedTwins);
        }

        boolean forceCreation = conf.connector().forceCreation();
        LOGGER.info("Needing to make: {}", twinsToBuild.get());
        List<CompletableFuture<UpsertTwinResponse>> opResults = Lists.newArrayList();
        foundChargers.forEach(evCharger -> {
            MakeResult result = makeEvChargerTwin(forceCreation, evCharger, sim, ioticsApi, listedTwins);
            result.maybeFuture().ifPresent(opResults::add);
            Arrays.stream(evCharger.connections()).toList().forEach(connection -> {
                MakeResult anotherResult = makeConnectionTwin(forceCreation, (EvChargerTwin) result.twin, connection, result.did(), sim, ioticsApi, listedTwins);
                anotherResult.maybeFuture().ifPresent(opResults::add);
            });
        });
        for (CompletableFuture<UpsertTwinResponse> future : opResults) {
            future.join();
        }
        apiExecutor.shutdown();
        System.exit(0);
    }

    private static void deleteAll(List<EvCharger> foundChargers, SimpleIdentityManager sim, IoticsApi ioticsApi, ExecutorService apiExecutor, Map<String, String> listedTwins) throws InterruptedException {
        LOGGER.info("Force deleting twins managed by this connector [count={}]", twinsToBuild.get());
        AtomicInteger count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(foundChargers.size());
        foundChargers.forEach(evCharger -> {
            EvChargerTwin twin = new EvChargerTwin(evCharger, sim, ioticsApi);
            String did = twin.getMyIdentity().did();
            if(listedTwins.containsKey(did)) {
                ListenableFuture<DeleteTwinResponse> fut = deleteTwin(did, sim, ioticsApi);
                fut.addListener(() -> {
                    LOGGER.info("Del station complete {}/{} [did={}]", count.get(), twinsToBuild.get(), did);
                    latch.countDown();
                    count.incrementAndGet();
                }, apiExecutor);
            } else {
                LOGGER.info("Skipping station {}/{} [did={}]", count.get(), twinsToBuild.get(), did);
                count.incrementAndGet();
            }
            Arrays.stream(evCharger.connections()).toList().forEach(connection -> {
                EvConnectionTwin connectionTwin = new EvConnectionTwin(connection, twin, sim, ioticsApi);
                String cDid = connectionTwin.getMyIdentity().did();
                if(listedTwins.containsKey(cDid)) {
                    ListenableFuture<DeleteTwinResponse> cFut = deleteTwin(cDid, sim, ioticsApi);
                    cFut.addListener(() -> {
                        LOGGER.info("Del connection complete {}/{} [did={}]", count.get(), twinsToBuild.get(), did);
                        latch.countDown();
                        count.incrementAndGet();
                    }, apiExecutor);
                } else {
                    LOGGER.info("Skipping connection {}/{} [did={}]", count.get(), twinsToBuild.get(), did);
                    count.incrementAndGet();
                }
            });
        });
        LOGGER.info("Awaiting for all deletions");
        latch.await();
        System.exit(0);
    }

    static ListenableFuture<DeleteTwinResponse> deleteTwin(String did, SimpleIdentityManager sim, IoticsApi ioticsApi) {
        return ioticsApi.twinAPIFuture().deleteTwin(DeleteTwinRequest.newBuilder()
                .setHeaders(Builders.newHeadersBuilder(sim.agentIdentity()))
                .setArgs(DeleteTwinRequest.Arguments.newBuilder()
                        .setTwinId(TwinID.newBuilder()
                                .setId(did)
                                .build())
                        .build())
                .build());
    }

    private static MakeResult makeTwin(boolean forceCreation, Map<String, String> listedTwins, AbstractTwin twin) {
        String name = twin.getClass().getName();
        String did = twin.getMyIdentity().did();

        MakeResult returnValue;

        RetryPolicyBuilder<Object> retryPolicyBuilder = RetryPolicy.builder()
                .handle(StatusRuntimeException.class).handleIf((e) -> {
                    StatusRuntimeException sre = (StatusRuntimeException) e;
                    return sre.getStatus() == Status.DEADLINE_EXCEEDED ||
                            sre.getStatus() == Status.UNAUTHENTICATED ||
                            sre.getStatus() == Status.UNAVAILABLE;
                }).withDelay(Duration.ofSeconds(10L))
                .withMaxRetries(-1)
                .withJitter(Duration.ofMillis(3000L));


        LOGGER.info("Upsert starting for [class={}. did={}]", name, did);
        if (!forceCreation && listedTwins.containsKey(did)) {
            LOGGER.info("Upsert skipped - already present [class={}, did={}]", name, did);
            twinsMade.incrementAndGet();
            returnValue = new MakeResult(did, Optional.empty(), twin);
        } else {
            CheckedSupplier<UpsertTwinResponse> operation = () -> twin.upsert().get();
            CompletableFuture<UpsertTwinResponse> fut = Failsafe.with(retryPolicyBuilder.build()).getAsync(operation);
            fut.thenAccept(upsertTwinResponse -> {
                        LOGGER.info("Upsert complete for {}/{} [class={}, did={}, responseRef={}]", twinsMade.incrementAndGet(), twinsToBuild.get(), name, did, upsertTwinResponse.getHeaders().getClientRef());
                    })
                    .exceptionally(e -> {
                        LOGGER.error("Upsert result error [class={}, did={}]", name, did, e.getCause());
                        return null;
                    });
            returnValue = new MakeResult(did, Optional.of(fut), twin);
        }
        return returnValue;
    }

    private static MakeResult makeEvChargerTwin(Boolean forceCreation, EvCharger evCharger, SimpleIdentityManager sim, IoticsApi ioticsApi, Map<String, String> listedTwins) {
        EvChargerTwin twin = new EvChargerTwin(evCharger, sim, ioticsApi);
        return makeTwin(forceCreation, listedTwins, twin);
    }

    private static MakeResult makeConnectionTwin(Boolean forceCreation, EvChargerTwin evChargerTwin, Connection connection, String parentDid, SimpleIdentityManager sim, IoticsApi ioticsApi, Map<String, String> listedTwins) {
        EvConnectionTwin twin = new EvConnectionTwin(connection, evChargerTwin, sim, ioticsApi);
        return makeTwin(forceCreation, listedTwins, twin);
    }

    private static Map<String, String> loadKnownTwins(SimpleIdentityManager sim, IoticsApi ioticsApi) throws InterruptedException, ExecutionException {
        Map<String, String> listedTwins = new HashMap<>();
        ListenableFuture<ListAllTwinsResponse> listFuture = ioticsApi.twinAPIFuture()
                .listAllTwins(ListAllTwinsRequest.newBuilder()
                        .setHeaders(Builders.newHeadersBuilder(sim.agentIdentity())
                                .build()).build());
        LOGGER.info("listing known space twins");
        ListAllTwinsResponse listResponse = listFuture.get();
        listResponse.getPayload().getTwinsList().forEach(twinDetails -> {
            listedTwins.put(twinDetails.getTwinId().getId(), "");
        });
        return listedTwins;
    }

}