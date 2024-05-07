package com.iotics.demo;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Timestamp;
import com.iotics.api.*;
import com.iotics.demo.configuration.Configuration;
import com.iotics.demo.iotics.SubscriberCarTwin;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import smartrics.iotics.host.HostEndpoints;
import smartrics.iotics.host.HttpServiceRegistry;
import smartrics.iotics.host.IoticsApi;
import smartrics.iotics.identity.SimpleIdentityManager;

import java.util.ArrayList;
import java.util.function.Consumer;

import static com.iotics.demo.iotics.Const.*;
import static com.iotics.demo.iotics.Tools.newIoticsApi;
import static com.iotics.demo.iotics.Tools.newSimpleIdentityManager;

/**
 * Subscriber app - it represents a car that needs to find compatible charging connections.
 */
public class EvSubscriber {
    private static final Logger LOGGER = LogManager.getLogger(EvSubscriber.class);
    private final SubscriberCarTwin twin;

    public EvSubscriber(SubscriberCarTwin twin) {
        this.twin = twin;
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = Configuration.loadConfiguration("src/main/resources/shared.yaml");
        HttpServiceRegistry sr = new HttpServiceRegistry(conf.space().name());
        HostEndpoints endpoints = sr.find();

        SimpleIdentityManager sim = newSimpleIdentityManager(conf, endpoints.resolver());
        IoticsApi ioticsApi = newIoticsApi(sim, endpoints.grpc(), conf.identity().tokenDuration());

        SubscriberCarTwin twin = new SubscriberCarTwin(ioticsApi, sim, "Fiat", "9891238909543", "500");
        twin.updateLocation(52.12471, -0.17315);

        LOGGER.info("Needing to make subscriber: {}", twin);
        ListenableFuture<UpsertTwinResponse> resultFut = twin.upsert();

        UpsertTwinResponse result = resultFut.get();
        LOGGER.info("Subscriber creation complete: {}", result.getPayload().getTwinId());

        EvSubscriber sub = new EvSubscriber(twin);
        sub.findAndBind();

    }

    private void findAndBind() {
        SearchRequest.Payload payload = newSearchRequestPayload();
        StreamObserver<SearchResponse.TwinDetails> so = newFollowingStreamObserver(payload);
        twin.search(payload, so);
    }

    private SearchRequest.Payload newSearchRequestPayload() {
        return SearchRequest.Payload.newBuilder()
                .setResponseType(ResponseType.FULL)
                .setExpiryTimeout(Timestamp.newBuilder().setSeconds(5).build())
                .setFilter(SearchRequest.Payload.Filter.newBuilder()
//                        .addProperties(Property.newBuilder()
//                                .setKey(RDF + "type")
//                                .setUriValue(Uri.newBuilder().setValue(ONT_EV + EV_CONNECTION).build()))
//                        .addProperties(Property.newBuilder().setKey(ONT_EV + "isOperational")
//                                .setLiteralValue(Literal.newBuilder().setDataType("boolean").setValue("true").build()).build())
//                        .addProperties(Property.newBuilder()
//                                .setKey(ONT_EV + "connectionTypeID")
//                                .setLiteralValue(Literal.newBuilder().setValue("25").build()).build())
                        .setLocation(GeoCircle.newBuilder()
                                .setRadiusKm(1) // search within 1Km
                                .setLocation(GeoLocation.newBuilder()
                                        .setLat(twin.location().getLat())
                                        .setLon(twin.location().getLon())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private StreamObserver<SearchResponse.TwinDetails> newFollowingStreamObserver(SearchRequest.Payload payload) {
        return new StreamObserver<>() {
            final ArrayList<SearchResponse.TwinDetails> results = Lists.newArrayList();

            @Override
            public void onNext(SearchResponse.TwinDetails twinDetails) {
                LOGGER.info("Found twin [n={}]", twinDetails);
                // follow all feeds of all twins found
                results.add(twinDetails);
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.error("error when receiving search response", throwable);
            }

            @Override
            public void onCompleted() {
                LOGGER.info("search response completed");
                if (results.isEmpty()) {
                    LOGGER.warn("No twins found matching your filters [filter={}]", payload.getFilter());
                    return;
                }
                GeoLocation carLocation = GeoLocation.newBuilder()
                        .setLat(EvSubscriber.this.twin.location().getLat())
                        .setLon(EvSubscriber.this.twin.location().getLon())
                        .build();
                if (results.size() > 1) {
                    // sort the list to find the closest EV charger
                    results.sort((t1, t2) -> {
                        double distanceFrom1 = HaversineDistance.apply(t1.getLocation(), carLocation);
                        double distanceFrom2 = HaversineDistance.apply(t2.getLocation(), carLocation);
                        return (int) (distanceFrom2 - distanceFrom1);
                    });
                }
                SearchResponse.TwinDetails closest = results.getFirst();
                LOGGER.info("Following closer twin [did={}]", closest.getTwinId().getId());
                EvSubscriber.this.follow(closest, feedData -> LOGGER.info("Twin share: [mime={}, data={}]", feedData.getMime(), feedData.getData().toStringUtf8()));
                // Follow ALL for other use cases
//                searchResponse.getPayload().getTwinsList().forEach(twinDetails -> EvSubscriber.this.follow(twinDetails, new Consumer<FeedData>() {
//                    @Override
//                    public void accept(FeedData feedData) {
//                        LOGGER.info("Twin share: [twin={}, mime={}, data={}]", twinDetails.getTwinId().getId(), feedData.getMime(), feedData.getData().toStringUtf8());
//                    }
//                }));
            }
        };
    }

    private void follow(SearchResponse.TwinDetails twinDetails, Consumer<FeedData> consumer) {
        TwinID twinId = twinDetails.getTwinId();
        twinDetails.getFeedsList().forEach(feedDetails -> {
            LOGGER.info("Found feed [feedId={}, did={}, host={}]", feedDetails.getFeedId().getId(), twinId.getId(), twinId.getHostId());
            follow(feedDetails, consumer);
        });
    }

    private void follow(SearchResponse.FeedDetails feedDetails, Consumer<FeedData> consumer) {
        twin.followNoRetry(feedDetails.getFeedId(), new StreamObserver<FetchInterestResponse>() {
            @Override
            public void onNext(FetchInterestResponse fetchInterestResponse) {
                consumer.accept(fetchInterestResponse.getPayload().getFeedData());
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.error("exception when following {}", feedDetails.getFeedId(), throwable);
            }

            @Override
            public void onCompleted() {
                LOGGER.info("completed following {}", feedDetails.getFeedId());
            }
        });
    }

    // https://en.wikipedia.org/wiki/Haversine_formula
    private static final class HaversineDistance {
        // Radius of the Earth in kilometers
        private static final double RADIUS = 6371.0088;

        // Method to calculate distance between two points using Haversine formula
        public static double apply(GeoLocation point, GeoLocation referencePoint) {
            double lat1 = Math.toRadians(referencePoint.getLat());
            double lon1 = Math.toRadians(referencePoint.getLon());
            double lat2 = Math.toRadians(point.getLat());
            double lon2 = Math.toRadians(point.getLon());

            double dLat = lat2 - lat1;
            double dLon = lon2 - lon1;

            double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

            return RADIUS * c;
        }
    }
}