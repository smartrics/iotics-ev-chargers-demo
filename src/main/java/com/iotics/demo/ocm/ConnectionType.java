package com.iotics.demo.ocm;

import com.google.gson.annotations.SerializedName;

public record ConnectionType(
        @SerializedName("FormalName") String formalName,
        @SerializedName("IsDiscontinued") Boolean isDiscontinued,
        @SerializedName("IsObsolete") Boolean isObsolete,
        @SerializedName("ID") Integer id,
        @SerializedName("Title") String title
) {}

