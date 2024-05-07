package com.iotics.demo.ocm;

import com.google.gson.annotations.SerializedName;

public record Country(
        @SerializedName("ISOCode") String isoCode,
        @SerializedName("ContinentCode") String continentCode,
        @SerializedName("ID") Integer id,
        @SerializedName("Title") String title
) {}