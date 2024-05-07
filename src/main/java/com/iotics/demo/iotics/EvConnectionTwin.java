package com.iotics.demo.iotics;

import com.iotics.api.GeoLocation;
import com.iotics.demo.ocm.Connection;
import smartrics.iotics.connectors.twins.*;
import smartrics.iotics.connectors.twins.annotations.*;
import smartrics.iotics.host.IoticsApi;
import smartrics.iotics.host.UriConstants;
import smartrics.iotics.identity.SimpleIdentityManager;

import static com.iotics.demo.iotics.Const.EV_CONNECTION;
import static com.iotics.demo.iotics.Const.ONT_EV;
import static com.iotics.demo.iotics.Tools.getRandomBoolean;
import static com.iotics.demo.iotics.Tools.sf;

public class EvConnectionTwin extends AbstractTwin implements MappableMaker, MappablePublisher, AnnotationMapper {
    @UriProperty(iri = UriConstants.RDFProperty.Type)
    private final static String type = ONT_EV + EV_CONNECTION;

    @UriProperty(iri = UriConstants.IOTICSProperties.HostAllowListName)
    private final String visibility = UriConstants.IOTICSProperties.HostAllowListValues.ALL.toString();

    @LiteralProperty(iri = ONT_EV + "isOperational", dataType = XsdDatatype.boolean_)
    private final String isOperational;

    @LiteralProperty(iri = ONT_EV + "isUserSelectable", dataType = XsdDatatype.boolean_)
    private final String isUserSelectable;

    @StringLiteralProperty(iri = ONT_EV + "chargingStationDid")
    private final String chargingStationDid;

    @StringLiteralProperty(iri = ONT_EV + "chargingStationId")
    private final String chargingStationId;

    @LiteralProperty(iri = ONT_EV + "isFastChargeCapable", dataType = XsdDatatype.boolean_)
    private final String isFastChargeCapable;

    @StringLiteralProperty(iri = ONT_EV + "connectionTypeID")
    private final String connectionTypeID;

    @StringLiteralProperty(iri = ONT_EV + "connectionTypeFormalName")
    private final String connectionTypeFormalName;

    @StringLiteralProperty(iri = ONT_EV + "connectionTypeName")
    private final String connectionTypeName;

    @LiteralProperty(iri = ONT_EV + "isConnectionTypeObsolete", dataType = XsdDatatype.boolean_)
    private final String isConnectionTypeObsolete;

    @LiteralProperty(iri = ONT_EV + "isConnectionTypeDiscontinued", dataType = XsdDatatype.boolean_)
    private final String isConnectionTypeDiscontinued;

    @StringLiteralProperty(iri = ONT_EV + "currentType")
    private final String currentType;

    @StringLiteralProperty(iri = ONT_EV + "currentTypeID")
    private final String currentTypeID;

    @LiteralProperty(iri = ONT_EV + "ampere", dataType = XsdDatatype.decimal)
    private final String ampere;

    @LiteralProperty(iri = ONT_EV + "voltage", dataType = XsdDatatype.decimal)
    private final String voltage;

    @LiteralProperty(iri = ONT_EV + "powerKW", dataType = XsdDatatype.decimal)
    private final String powerKW;

    @LiteralProperty(iri = ONT_EV + "quantity", dataType = XsdDatatype.integer)
    private final String quantity;

    @StringLiteralProperty(iri = UriConstants.RDFSProperty.Label)
    private final String label;

    @StringLiteralProperty(iri = UriConstants.RDFSProperty.Comment)
    private final String comment;

    @Location
    private final GeoLocation location;

    public static String connectionUUID(String evChargerUUID, Integer connectionId) {
        return evChargerUUID + "_" + connectionId;
    }

    public EvConnectionTwin(Connection connection, EvChargerTwin evChargerTwin, SimpleIdentityManager sim, IoticsApi api) {
        super(api, sim, sim.newTwinIdentityWithControlDelegation(connectionUUID(evChargerTwin.uuid(), connection.id()), "#masterKey"));

        isOperational = connection.makeOperationalStatus();
        isUserSelectable = connection.makeUserSelectableStatus();
        chargingStationDid = evChargerTwin.getMyIdentity().did();
        chargingStationId = evChargerTwin.uuid();

        isFastChargeCapable = sf(() -> connection.level().isFastChargeCapable(), "false").toString();

        connectionTypeID = connection.connectionTypeID().toString();
        connectionTypeFormalName = sf(() -> connection.connectionType().formalName(), "unknown").toString();
        connectionTypeName = connection.connectionType().title();
        isConnectionTypeObsolete = sf(() -> connection.connectionType().isObsolete(), "true").toString();
        isConnectionTypeDiscontinued = sf(() -> connection.connectionType().isDiscontinued(), "true").toString();

        currentType = sf(() -> connection.currentType().title(), "unknown").toString();
        currentTypeID = sf(() -> connection.currentType().id(), "unknown").toString();

        ampere = sf(connection::amps, "0").toString();
        voltage = sf(connection::voltage, "0").toString();
        powerKW = sf(connection::powerKW, "0").toString();
        quantity = sf(connection::quantity, "1").toString();

        label = connection.makeLabel();
        comment = connection.makeComment(evChargerTwin.label());
        location = evChargerTwin.location();
    }

    @Override
    public Mapper getMapper() {
        return this;
    }

    @Feed(id = "status")
    public OperationalStatus newOperationalStatus() {
        return new OperationalStatus(getRandomBoolean(0.99), getRandomBoolean(0.5));
    }

    public record OperationalStatus(
            @PayloadValue(dataType = "boolean", comment = "true if operational") boolean isOperational,
            @PayloadValue(dataType = "boolean", comment = "true if user selectable") boolean isUserSelectable) {
        @StringLiteralProperty(iri = UriConstants.RDFSProperty.Label)
        public static final String label = "OperationalStatus";
        @StringLiteralProperty(iri = UriConstants.RDFSProperty.Comment)
        public static final String comment = "Current operational status of this connection";
    }

}
