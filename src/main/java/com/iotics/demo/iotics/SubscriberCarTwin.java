package com.iotics.demo.iotics;

import com.iotics.api.GeoLocation;
import smartrics.iotics.connectors.twins.*;
import smartrics.iotics.connectors.twins.annotations.*;
import smartrics.iotics.host.IoticsApi;
import smartrics.iotics.host.UriConstants;
import smartrics.iotics.identity.SimpleIdentityManager;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public class SubscriberCarTwin extends AbstractTwin implements MappableMaker, Follower, Publisher, AnnotationMapper, Searcher {
    @UriProperty(iri = UriConstants.RDFProperty.Type)
    private final static String type = "https://schema.org/Vehicle";
    @UriProperty(iri = UriConstants.IOTICSProperties.HostAllowListName)
    private final String visibility = UriConstants.IOTICSProperties.HostAllowListValues.ALL.toString();
    @StringLiteralProperty(iri = "https://schema.org/serialNumber")
    private final String serial;
    @StringLiteralProperty(iri = "https://schema.org/manufacturer")
    private final String manufacturer;
    @StringLiteralProperty(iri = "https://schema.org/model")
    private final String model;
    @StringLiteralProperty(iri = "https://schema.org/identifier")
    private final UUID uuid;
    @StringLiteralProperty(iri = UriConstants.RDFSProperty.Comment)
    private final String comment;
    @Location
    private GeoLocation location;


    public SubscriberCarTwin(IoticsApi api, SimpleIdentityManager sim, String manufacturer, String serial, String model) {
        this(api, sim, manufacturer, serial, model, newUUIDFromString(String.join("_", new String[]{manufacturer, serial, model})), -1, -1);
    }

    private SubscriberCarTwin(IoticsApi api, SimpleIdentityManager sim, String manufacturer, String serial, String model, UUID uuid, double lat, double lon) {
        super(api, sim, sim.newTwinIdentityWithControlDelegation(uuid.toString().replace("-", ""), "#masterKey"));
        this.serial = serial;
        this.manufacturer = manufacturer;
        this.model = model;
        this.uuid = uuid;
        this.updateLocation(lat, lon);
        this.comment = "EvChargers - A car navigating the world: " + label();
    }

    private static UUID newUUIDFromString(String s) {
        byte[] seedBytes = s.getBytes(StandardCharsets.UTF_8);
        return UUID.nameUUIDFromBytes(seedBytes);
    }

    @StringLiteralProperty(iri = UriConstants.RDFSProperty.Label)
    public String label() {
        return String.format("%s %s (%s)", manufacturer, model, serial);
    }

    public String serial() {
        return serial;
    }

    public String manufacturer() {
        return manufacturer;
    }

    public String model() {
        return model;
    }

    public UUID uuid() {
        return uuid;
    }

    public GeoLocation location() {
        return this.location;
    }

    public GeoLocation updateLocation(double lat, double lon) {
        this.location = GeoLocation.newBuilder().setLon(lat).setLat(lon).build();
        return this.location;
    }

    @Feed(id = "status")
    public CarLocation lastKnownLocation() {
        return new CarLocation(this.location.getLat(), this.location.getLon());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriberCarTwin that = (SubscriberCarTwin) o;
        return Objects.equals(uuid.toString(), that.uuid.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid.toString());
    }

    @Override
    public String toString() {
        return "SubscriberCar{" +
                "serial='" + serial + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", model='" + model + '\'' +
                ", uuid=" + uuid +
                ", location={lat=" + this.location.getLat() + ", lon=" + this.location.getLon() + "}" +
                '}';
    }

    @Override
    public Mapper getMapper() {
        return this;
    }

    public record CarLocation(
            @PayloadValue(dataType = "decimal", comment = "latitude") double lat,
            @PayloadValue(dataType = "decimal", comment = "longitude") double lon) {
        @StringLiteralProperty(iri = UriConstants.RDFSProperty.Label)
        public static final String label = "CarLocation";
        @StringLiteralProperty(iri = UriConstants.RDFSProperty.Comment)
        public static final String comment = "Last known location of the car";
    }
}
