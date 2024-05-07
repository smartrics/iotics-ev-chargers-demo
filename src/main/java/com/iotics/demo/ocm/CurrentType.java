package com.iotics.demo.ocm;

import com.google.gson.annotations.SerializedName;

public record CurrentType(
        @SerializedName("Description") String description,
        @SerializedName("ID") Integer id,
        @SerializedName("Title") String title
) {}