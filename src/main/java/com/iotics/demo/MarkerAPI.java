package com.iotics.demo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.iotics.api.*;
import com.iotics.demo.configuration.Configuration;
import io.grpc.stub.StreamObserver;
import smartrics.iotics.connectors.twins.Searcher;
import smartrics.iotics.host.HostEndpoints;
import smartrics.iotics.host.HttpServiceRegistry;
import smartrics.iotics.host.IoticsApi;
import smartrics.iotics.host.UriConstants;
import smartrics.iotics.identity.Identity;
import smartrics.iotics.identity.SimpleIdentityManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.iotics.demo.iotics.Tools.newIoticsApi;
import static com.iotics.demo.iotics.Tools.newSimpleIdentityManager;
import static spark.Spark.*;

public class MarkerAPI {

    private static SearcherImpl searcher;

    public static void main(String[] args) throws Exception {
        Configuration conf = Configuration.loadConfiguration("src/main/resources/shared.yaml");
        HttpServiceRegistry sr = new HttpServiceRegistry(conf.space().name());
        HostEndpoints endpoints = sr.find();

        SimpleIdentityManager sim = newSimpleIdentityManager(conf, endpoints.resolver());
        IoticsApi ioticsApi = newIoticsApi(sim, endpoints.grpc(), conf.identity().tokenDuration());

        Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

        searcher = new SearcherImpl(ioticsApi, sim);

        port(8000); // Spark will run on port 8080

        // Configure Spark to serve static files
        staticFiles.location("/public"); // Static files are in 'src/main/resources/public'

        get("/markers", (request, response) -> {
            double lat = Double.parseDouble(request.queryParams("lat"));
            double lon = Double.parseDouble(request.queryParams("lon"));
            double radius = Double.parseDouble(request.queryParams("radius"));

            if (radius > 25.0) {
                radius = 25.0;
            }

            List<Marker> markers = generateMarkers(lat, lon, radius);
            response.type("application/json");

            return gson.toJson(markers);
        });
    }

    private static List<Marker> generateMarkers(double lat, double lon, double radius) {
        List<SearchResponse.TwinDetails> details = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        searcher.search(SearchRequest.Payload.newBuilder()
                .setResponseType(ResponseType.FULL)
                .setFilter(SearchRequest.Payload.Filter.newBuilder()
                        .setLocation(GeoCircle.newBuilder()
                                .setRadiusKm(radius)
                                .setLocation(GeoLocation.newBuilder()
                                        .setLat(lat)
                                        .setLon(lon)
                                        .build())
                                .build())
                        .build())
                .build(), Duration.ofSeconds(3), new StreamObserver<>() {
            @Override
            public void onNext(SearchResponse.TwinDetails twinDetails) {
                details.add(twinDetails);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("onError: " + throwable.getMessage());
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("onCompleted");
                latch.countDown();
            }
        });

        try {
            System.out.println("awaiting");
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        List<SearchResponse.TwinDetails> stations = details.stream()
                .filter(p -> propValue(UriConstants.RDFProperty.Type,
                        p.getPropertiesList()).split("#")[1].endsWith("ElectricVehicleChargingStation")).toList();

        return stations.stream().map(twinDetails -> {
            List<Property> pl = twinDetails.getPropertiesList();
            String type = propValue(UriConstants.RDFProperty.Type, pl).split("#")[1];
            String label = propValue(UriConstants.RDFSProperty.Comment, pl);
            String info1 = "number of points: " + propValue("https://data.iotics.com/ont/ev#numberOfPoints", pl);
            String info2 = "operational: " + propValue("https://data.iotics.com/ont/ev#isOperational", pl);
            String info3 = "last verified: " + propValue("https://data.iotics.com/ont/ev#dateLastVerified", pl);
            double lat1 = twinDetails.getLocation().getLat();
            double lon1 = twinDetails.getLocation().getLon();
            return new Marker(lat1, lon1, type, label, info1, info2, info3);
        }).toList();
    }

    private static String propValue(String key, List<Property> list) {
        return list.stream().filter(property -> property.getKey().equals(key))
                .map(property -> {
                    if (property.hasLiteralValue()) return property.getLiteralValue().getValue();
                    if (property.hasStringLiteralValue()) return property.getStringLiteralValue().getValue();
                    if (property.hasLangLiteralValue()) return property.getLangLiteralValue().getValue();
                    if (property.hasUriValue()) return property.getUriValue().getValue();
                    return "unknown";
                }).findFirst().orElse("unknown");
    }


    private static class SearcherImpl implements Searcher {

        private final IoticsApi api;
        private final Identity myIdentity;
        private final Identity agentIdentity;

        SearcherImpl(IoticsApi ioticsApi, SimpleIdentityManager sim) {
            this.api = ioticsApi;
            this.myIdentity = sim.newTwinIdentity("makerApi-123", "#twin-0");
            this.agentIdentity = sim.agentIdentity();
        }

        @Override
        public IoticsApi ioticsApi() {
            return api;
        }

        @Override
        public Identity getMyIdentity() {
            return myIdentity;
        }

        @Override
        public Identity getAgentIdentity() {
            return agentIdentity;
        }


    }

    // Marker class definition, assuming you're passing more data in constructor
    record Marker(double latitude, double longitude, String type, String label, String info1, String info2,
                  String info3) {
    }

}
