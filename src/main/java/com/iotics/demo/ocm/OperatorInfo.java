package com.iotics.demo.ocm;

import com.google.gson.annotations.SerializedName;

import java.net.URI;
import java.net.URL;

public record OperatorInfo(
        @SerializedName("WebsiteURL") URL websiteURL,
        @SerializedName("Comments") String comments,
        @SerializedName("PhonePrimaryContact") String phonePrimaryContact,
        @SerializedName("PhoneSecondaryContact") String phoneSecondaryContact,
        @SerializedName("IsPrivateIndividual") Boolean isPrivateIndividual,
        @SerializedName("AddressInfo") AddressInfo addressInfo,
        @SerializedName("BookingURL") URL bookingURL,
        @SerializedName("ContactEmail") URI contactEmail,
        @SerializedName("FaultReportEmail") URI faultReportEmail,
        @SerializedName("IsRestrictedEdit") Boolean isRestrictedEdit,
        @SerializedName("ID") Integer id,
        @SerializedName("Title") String title
) {}
