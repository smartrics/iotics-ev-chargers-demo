package com.iotics.demo.ocm;

import com.google.gson.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OcmApi {
    public static List<EvCharger> loadEvChargersFromURL(String urlString) throws IOException {
        // https://api.openchargemap.io/v3/poi/?output=json&countrycode=GB&maxresults=1000&key=<your key>
        try {
            // use the swagger client
            URL url = URI.create(urlString).toURL();
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            return parseEvChargersJson(reader);
        } catch (IOException e) {
            throw new IOException("Error loading EV chargers from URL: " + e.getMessage());
        }
    }

    public static List<EvCharger> loadEvChargersFromPath(String filePath) throws IOException {
        return parseEvChargersJson(new FileReader(filePath));
    }

    public static List<EvCharger> parseEvChargersJson(Reader reader) {
        List<EvCharger> evChargers = new ArrayList<>();

        Gson gson = new GsonBuilder().registerTypeAdapter(ZonedDateTime.class, (JsonDeserializer<ZonedDateTime>) (json, typeOfT, context) -> {
                    String dateString = json.getAsString();
                    return ZonedDateTime.parse(dateString.trim(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                }).registerTypeAdapter(URL.class, (JsonDeserializer<URL>) (json, typeOfT, context) -> {
                    String url = json.getAsString();
                    URL uurl;
                    try {
                        uurl = URI.create(url.trim()).toURL();
                    } catch (MalformedURLException e) {
                        uurl = null;
                    }
                    return uurl;
                })

                .create();
        JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

        // Iterate over the JSON array and deserialize each object into an EvCharger
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonElement json = jsonArray.get(i);
            try {
                EvCharger e = gson.fromJson(json, EvCharger.class);
                evChargers.add(e);
            } catch (Exception ex) {

            }
        }

        return evChargers;
    }
}
