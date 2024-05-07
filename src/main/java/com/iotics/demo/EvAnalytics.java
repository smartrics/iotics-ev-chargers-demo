package com.iotics.demo;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.iotics.api.Scope;
import com.iotics.api.SparqlQueryRequest;
import com.iotics.api.SparqlQueryResponse;
import com.iotics.api.UpsertTwinResponse;
import com.iotics.demo.configuration.Configuration;
import com.iotics.demo.iotics.AnalyticsTwin;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import smartrics.iotics.host.Builders;
import smartrics.iotics.host.HostEndpoints;
import smartrics.iotics.host.HttpServiceRegistry;
import smartrics.iotics.host.IoticsApi;
import smartrics.iotics.identity.Identity;
import smartrics.iotics.identity.SimpleIdentityManager;
import smartrics.iotics.identity.resolver.HttpResolverClient;
import smartrics.iotics.identity.resolver.ResolverClient;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.iotics.demo.iotics.Tools.newIoticsApi;
import static com.iotics.demo.iotics.Tools.newSimpleIdentityManager;

/**
 * Runs simple analytics on an IOTICS Federated Knowledge Graph.
 */
public class EvAnalytics {
    private static final Logger LOGGER = LogManager.getLogger(EvAnalytics.class);
    private static final String powerConsumptionByOperatorTown = """
            PREFIX ev: <https://data.iotics.com/ont/ev#>

            SELECT ?operator ?addressStateOrProvince ?town (SUM(?powerKW) AS ?totalPowerKW)
            WHERE {
              ?chargingStation a ev:ElectricVehicleChargingStation ;
                               ev:operator ?operator ;
                               ev:town ?town ;
                               ev:addressStateOrProvince ?addressStateOrProvince ;
                               ev:isOperational true ;
                               ev:uuid ?uuid .
              ?connectionPoint a ev:ElectricVehicleChargingStationConnection ;
                               ev:isOperational true ;
                               ev:chargingStationId ?uuid ;
                               ev:powerKW ?powerKW .
            }
            GROUP BY ?operator ?addressStateOrProvince ?town
            ORDER BY ?operator ?addressStateOrProvince ?town
            """;
    private static final String operationalStations = """
            PREFIX ev: <https://data.iotics.com/ont/ev#>

            SELECT ?status (COUNT(?s) AS ?count)
            WHERE {
                ?s a ev:ElectricVehicleChargingStationConnection ;
                   ev:isOperational ?isOperational .
                BIND(IF(?isOperational, "Operational", "Non Operational") AS ?status)
            }
            GROUP BY ?status""";
    private final IoticsApi ioticsApi;

    public EvAnalytics(IoticsApi ioticsApi) {
        this.ioticsApi = ioticsApi;
    }

    public static void main(String[] args) throws Exception {
//        runQuery(powerConsumptionByOperatorTown);
        runQuery(operationalStations);
    }

    public static void runQuery(String query) throws IOException, InterruptedException, ExecutionException {
        Configuration conf = Configuration.loadConfiguration("src/main/resources/shared.yaml");
        HttpServiceRegistry sr = new HttpServiceRegistry(conf.space().name());
        HostEndpoints endpoints = sr.find();

        SimpleIdentityManager sim = newSimpleIdentityManager(conf, endpoints.resolver());

        IoticsApi ioticsApi = newIoticsApi(sim, endpoints.grpc(), conf.identity().tokenDuration());

        Identity myIdentity = sim.newTwinIdentityWithControlDelegation("evAnalytics", "#evAnalytics");

        AnalyticsTwin twin = new AnalyticsTwin(ioticsApi, sim, myIdentity);
        LOGGER.info("Needing to make twin of analytics platform: {}", twin);
        ListenableFuture<UpsertTwinResponse> resultFut = twin.upsert();

        UpsertTwinResponse result = resultFut.get();
        LOGGER.info("Analytics creation complete: {}", result.getPayload().getTwinId());

        EvAnalytics sub = new EvAnalytics(ioticsApi);
        sub.query(query, sim.agentIdentity());
    }

    private static void printRow(String[] values, int[] colWidths) {
        StringBuilder rowBuilder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            String formattedValue = String.format("%-" + colWidths[i] + "s", values[i]);
            rowBuilder.append("| ").append(formattedValue).append(" ");
        }
        rowBuilder.append("|");
        System.out.println(rowBuilder);
    }

    private void query(String query, Identity agentId) {
        List<ByteString> chunks = Lists.newArrayList();
        CountDownLatch latch = new CountDownLatch(1);
        this.ioticsApi.metaAPI().sparqlQuery(SparqlQueryRequest.newBuilder().setHeaders(Builders.newHeadersBuilder(agentId)).setScope(Scope.LOCAL).setPayload(SparqlQueryRequest.Payload.newBuilder().setQuery(ByteString.copyFromUtf8(query)).build()).build(), new StreamObserver<>() {
            @Override
            public void onNext(SparqlQueryResponse sparqlQueryResponse) {
                System.out.println(sparqlQueryResponse.getPayload().getResultChunk().size());
                SparqlQueryResponse.Payload payload = sparqlQueryResponse.getPayload();
                chunks.add(payload.getResultChunk());
                LOGGER.info("Chunk: [seq={}, last={}, status={}, content={}]", payload.getSeqNum(), payload.getLast(), payload.getStatus(), payload.getResultChunk().toStringUtf8());
                if (payload.getLast()) {
                    latch.countDown();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.warn("Error received", throwable);
            }

            @Override
            public void onCompleted() {
                LOGGER.info("Completed!");
            }
        });
        try {
            LOGGER.info("Waiting for latch!");
            latch.await();
            String joinedString = chunks.stream().map(ByteString::toStringUtf8).collect(Collectors.joining());

            // Print the joined string
            LOGGER.info("Full String: {}", joinedString);
            TableResult result = parse(joinedString);
            printTable(result);

        } catch (InterruptedException e) {
            LOGGER.warn("unable to complete query", e);
        }
    }

    private TableResult parse(String jsonString) {
        // Parse JSON data
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        JsonObject results = jsonObject.getAsJsonObject("results");
        JsonArray bindings = results.getAsJsonArray("bindings");

        // Extract variable names
        JsonArray varsArray = jsonObject.getAsJsonObject("head").getAsJsonArray("vars");
        List<String> vars = new ArrayList<>();
        for (JsonElement element : varsArray) {
            vars.add(element.getAsString());
        }

        // Create table headers
        String[] headers = vars.toArray(new String[0]);

        // Create table data
        List<String[]> tableData = new ArrayList<>();
        for (JsonElement element : bindings) {
            JsonObject binding = element.getAsJsonObject();
            String[] row = new String[vars.size()];
            for (int j = 0; j < vars.size(); j++) {
                JsonObject valueObject = binding.getAsJsonObject(vars.get(j));
                row[j] = valueObject.get("value").getAsString();
            }
            tableData.add(row);
        }

        return new TableResult(headers, tableData);
    }

    private void printTable(TableResult result) {
        // Calculate column widths
        int[] colWidths = new int[result.headers().length];
        for (int i = 0; i < result.headers().length; i++) {
            colWidths[i] = result.headers()[i].length();
            for (String[] row : result.tableData()) {
                if (row[i].length() > colWidths[i]) {
                    colWidths[i] = row[i].length();
                }
            }
        }

        // Print headers
        printRow(result.headers(), colWidths);

        // Print data rows
        for (String[] row : result.tableData()) {
            printRow(row, colWidths);
        }
    }

    private record TableResult(String[] headers, List<String[]> tableData) {
    }
}