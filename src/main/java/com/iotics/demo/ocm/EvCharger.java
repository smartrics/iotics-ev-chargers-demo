package com.iotics.demo.ocm;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.ZonedDateTime;

public record EvCharger(@SerializedName("DataProvider") DataProvider dataProvider,
                        @SerializedName("OperatorInfo") OperatorInfo operatorInfo,
                        @SerializedName("UsageType") UsageType usageType,
                        @SerializedName("StatusType") StatusType statusType,
                        @SerializedName("SubmissionStatus") SubmissionStatus submissionStatus,
                        @SerializedName("UserComments") UserComment[] userComments,
                        @SerializedName("PercentageSimilarity") Integer percentageSimilarity,
                        @SerializedName("MediaItems") Object mediaItems,
                        @SerializedName("IsRecentlyVerified") Boolean isRecentlyVerified,
                        @SerializedName("DateLastVerified") ZonedDateTime dateLastVerified,
                        @SerializedName("ID") Integer id, @SerializedName("UUID") String uuid,
                        @SerializedName("ParentChargePointID") Integer parentChargePointID,
                        @SerializedName("DataProviderID") Integer dataProviderID,
                        @SerializedName("DataProvidersReference") String dataProvidersReference,
                        @SerializedName("OperatorID") Integer operatorID,
                        @SerializedName("OperatorsReference") String operatorsReference,
                        @SerializedName("UsageTypeID") Integer usageTypeID,
                        @SerializedName("UsageCost") String usageCost,
                        @SerializedName("AddressInfo") AddressInfo addressInfo,
                        @SerializedName("Connections") Connection[] connections,
                        @SerializedName("NumberOfPoints") Integer numberOfPoints,
                        @SerializedName("GeneralComments") String generalComments,
                        @SerializedName("DatePlanned") ZonedDateTime datePlanned,
                        @SerializedName("DateLastConfirmed") ZonedDateTime dateLastConfirmed,
                        @SerializedName("StatusTypeID") Integer statusTypeID,
                        @SerializedName("DateLastStatusUpdate") ZonedDateTime dateLastStatusUpdate,
                        @SerializedName("MetadataValues") Object metadataValues,
                        @SerializedName("DataQualityLevel") Integer dataQualityLevel,
                        @SerializedName("DateCreated") ZonedDateTime dateCreated,
                        @SerializedName("SubmissionStatusTypeID") Integer submissionStatusTypeID) {
    @NotNull
    public String makeNumPoints() {
        String nop = "0";
        if(this.numberOfPoints() != null) {
            nop = this.numberOfPoints().toString();
        }
        return nop;
    }


    @NotNull
    public String makeLabel() {
        return "Op:" + this.operatorID() + ",ID:" + this.id() + ",LOC:" + this.addressInfo().id();
    }

    @NotNull
    public String makeOperationalStatus() {
        StatusType statusType = this.statusType();
        String isOperational;
        if(statusType == null || statusType.isOperational() == null) {
            isOperational = "false";
        } else {
            isOperational = statusType.isOperational().toString();
        }
        return isOperational;
    }

    @NotNull
    public String makeComment() {
        String operatorTitle = "Unknown Operator";
        String address = "Unknown Address";
        try {
            operatorTitle = this.operatorInfo().title();
        } catch (Exception ex) {
            // ignore
        }
        try {
            address = this.addressInfo().title() + ", " + this.addressInfo().postcode();
        } catch (Exception ex) {
            // ignore
        }
        return operatorTitle + " @ " + address;
    }

}
