package com.iotics.demo.ocm;

import com.google.gson.annotations.SerializedName;

public record Level(
        @SerializedName("Comments") String comments,
        @SerializedName("IsFastChargeCapable") Boolean isFastChargeCapable,
        @SerializedName("ID") Integer id,
        @SerializedName("Title") String title
) {}
