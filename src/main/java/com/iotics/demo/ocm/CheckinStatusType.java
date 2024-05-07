package com.iotics.demo.ocm;

import com.google.gson.annotations.SerializedName;

public record CheckinStatusType(
        @SerializedName("IsPositive") Boolean isPositive,
        @SerializedName("IsAutomatedCheckin") Boolean isAutomatedCheckin,
        @SerializedName("ID") Integer id,
        @SerializedName("Title") String title
) {
}
