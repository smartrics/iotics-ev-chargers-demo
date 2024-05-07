package com.iotics.demo.ocm;

import com.google.gson.annotations.SerializedName;

import java.net.URL;

public record AddressInfo(
        @SerializedName("ID") Integer id,
        @SerializedName("Title") String title,
        @SerializedName("AddressLine1") String addressLine1,
        @SerializedName("AddressLine2") String addressLine2,
        @SerializedName("Town") String town,
        @SerializedName("StateOrProvince") String stateOrProvince,
        @SerializedName("Postcode") String postcode,
        @SerializedName("CountryID") Integer countryID,
        @SerializedName("Country") Country country,
        @SerializedName("Latitude") Double latitude,
        @SerializedName("Longitude") Double longitude,
        @SerializedName("ContactTelephone1") String contactTelephone1,
        @SerializedName("ContactTelephone2") String contactTelephone2,
        @SerializedName("ContactEmail") String contactEmail,
        @SerializedName("AccessComments") String accessComments,
        @SerializedName("RelatedURL") URL relatedURL,
        @SerializedName("Distance") Integer distance,
        @SerializedName("DistanceUnit") Integer distanceUnit
) {}