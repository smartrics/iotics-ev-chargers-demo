package com.iotics.demo.iotics;

import com.iotics.api.GeoLocation;
import com.iotics.demo.ocm.EvCharger;
import smartrics.iotics.connectors.twins.*;
import smartrics.iotics.connectors.twins.annotations.*;
import smartrics.iotics.host.IoticsApi;
import smartrics.iotics.host.UriConstants;
import smartrics.iotics.identity.SimpleIdentityManager;

import java.time.format.DateTimeFormatter;

import static com.iotics.demo.iotics.Const.EV_STATION;
import static com.iotics.demo.iotics.Const.ONT_EV;
import static com.iotics.demo.iotics.Tools.*;

public class EvChargerTwin extends AbstractTwin implements MappableMaker, MappablePublisher, AnnotationMapper {
    @UriProperty(iri = UriConstants.RDFProperty.Type)
    private static String type = ONT_EV + EV_STATION;
    private final EvCharger evCharger;
    @UriProperty(iri = UriConstants.IOTICSProperties.HostAllowListName)
    private final String visibility = UriConstants.IOTICSProperties.HostAllowListValues.ALL.toString();
    @Location
    private final GeoLocation location;

    @StringLiteralProperty(iri = UriConstants.RDFSProperty.Label)
    private final String label;

    @StringLiteralProperty(iri = UriConstants.RDFSProperty.Comment)
    private final String comment;

    @LiteralProperty(iri = ONT_EV + "isPublic", dataType = XsdDatatype.boolean_)
    private final String isPublic;

    @LiteralProperty(iri = ONT_EV + "isOperational", dataType = XsdDatatype.boolean_)
    private final String isOperational;

    @LiteralProperty(iri = ONT_EV + "isRecentlyVerified", dataType = XsdDatatype.boolean_)
    private final String isRecentlyVerified;

    @LiteralProperty(iri = ONT_EV + "isLiveSubmission", dataType = XsdDatatype.boolean_)
    private final String isLiveSubmission;

    @LiteralProperty(iri = ONT_EV + "dateLastVerified", dataType = XsdDatatype.dateTime)
    private final String dateLastVerified;

    @LiteralProperty(iri = ONT_EV + "numberOfPoints", dataType = XsdDatatype.integer)
    private final String numberOfPoints;

    @StringLiteralProperty(iri = ONT_EV + "uuid")
    private final String uuid;

    @StringLiteralProperty(iri = ONT_EV + "operatorId")
    private final String operatorId;

    @StringLiteralProperty(iri = ONT_EV + "operator")
    private final String operator;

    @StringLiteralProperty(iri = ONT_EV + "town")
    private final String town;

    @StringLiteralProperty(iri = ONT_EV + "addressPostcode")
    private final String addressPostcode;

    @StringLiteralProperty(iri = ONT_EV + "addressStateOrProvince")
    private final String addressStateOrProvince;

    public EvChargerTwin(EvCharger evCharger, SimpleIdentityManager sim, IoticsApi api) {
        super(api, sim, sim.newTwinIdentityWithControlDelegation(evCharger.uuid(), "#masterKey"));
        this.evCharger = evCharger;
        this.label = evCharger.makeLabel();
        this.comment = evCharger.makeComment();
        this.location = GeoLocation.newBuilder().setLon(evCharger.addressInfo().longitude()).setLat(evCharger.addressInfo().latitude()).build();
        this.isPublic = Boolean.toString(evCharger.usageType().id() == 1);
        this.isOperational = evCharger.makeOperationalStatus();
        this.isRecentlyVerified = evCharger.isRecentlyVerified().toString();
        this.isLiveSubmission = evCharger.submissionStatus().isLive().toString();
        this.dateLastVerified = evCharger.dateLastVerified().format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        this.numberOfPoints = evCharger.makeNumPoints();
        this.uuid = evCharger.uuid();
        this.operator = sf(() -> evCharger.operatorInfo().title(), "unknown").toString();
        this.operatorId = sf(() -> evCharger.operatorInfo().id(), "unknown").toString();
        this.town = sf2(() -> evCharger.addressInfo().town(), "unknown").toString();
        this.addressPostcode = sf2(() -> evCharger.addressInfo().postcode(), "unknown").toString();
        this.addressStateOrProvince = sf2(() -> evCharger.addressInfo().stateOrProvince(), "unknown").toString();
    }

    @Override
    public Mapper getMapper() {
        return this;
    }

    public GeoLocation location() {
        return this.location;
    }

    public String label() {
        return this.label;
    }

    public String uuid() {
        return this.uuid;
    }

    @Feed(id = "status")
    public OperationalStatus newOperationalStatus() {
        return new OperationalStatus(getRandomBoolean(0.99), getRandomBoolean(0.999));
    }

    public record OperationalStatus(
            @PayloadValue(dataType = "boolean", comment = "true if operational") boolean isOperational,
            @PayloadValue(dataType = "boolean", comment = "true if recently verified") boolean isRecentlyVerified) {
        @StringLiteralProperty(iri = UriConstants.RDFSProperty.Label)
        public static final String label = "OperationalStatus";
        @StringLiteralProperty(iri = UriConstants.RDFSProperty.Comment)
        public static final String comment = "Current operational status of this station";
    }
}
