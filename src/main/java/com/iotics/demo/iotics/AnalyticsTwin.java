package com.iotics.demo.iotics;


import smartrics.iotics.connectors.twins.AbstractTwin;
import smartrics.iotics.connectors.twins.AnnotationMapper;
import smartrics.iotics.connectors.twins.MappableMaker;
import smartrics.iotics.connectors.twins.Mapper;
import smartrics.iotics.connectors.twins.annotations.StringLiteralProperty;
import smartrics.iotics.connectors.twins.annotations.UriProperty;
import smartrics.iotics.host.IoticsApi;
import smartrics.iotics.host.UriConstants;
import smartrics.iotics.identity.Identity;
import smartrics.iotics.identity.SimpleIdentityManager;

public class AnalyticsTwin extends AbstractTwin implements MappableMaker, AnnotationMapper {
    @UriProperty(iri = UriConstants.RDFProperty.Type)
    private final String type = "https://schema.org/SoftwareApplication";

    @StringLiteralProperty(iri = UriConstants.RDFSProperty.Label)
    public final String label = "EvChargers-Analytics4U, serial:#09786543546576";

    @StringLiteralProperty(iri = UriConstants.RDFSProperty.Comment)
    public final String comment = "EvChargers - Analytics platform: " + label;

    @UriProperty(iri = UriConstants.IOTICSProperties.HostAllowListName)
    public final String visibility = UriConstants.IOTICSProperties.HostAllowListValues.ALL.toString();

    public AnalyticsTwin(IoticsApi api, SimpleIdentityManager sim, Identity myIdentity) {
        super(api, sim, myIdentity);
    }

    @Override
    public Mapper getMapper() {
        return this;
    }

}
