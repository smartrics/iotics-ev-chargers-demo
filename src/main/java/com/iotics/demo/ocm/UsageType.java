package com.iotics.demo.ocm;

import com.google.gson.annotations.SerializedName;

public record UsageType(
        @SerializedName("IsPayAtLocation") Boolean isPayAtLocation,
        @SerializedName("IsMembershipRequired") Boolean isMembershipRequired,
        @SerializedName("IsAccessKeyRequired") Boolean isAccessKeyRequired,
        @SerializedName("ID") Integer id,
        @SerializedName("Title") String title
) {}
