package com.iotics.demo.ocm;

import com.google.gson.annotations.SerializedName;

public record DataProviderStatusType(
        @SerializedName("IsProviderEnabled") Boolean isProviderEnabled,
        @SerializedName("ID") Integer id,
        @SerializedName("Title") String title
) {}