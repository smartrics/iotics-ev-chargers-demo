package com.iotics.demo.ocm;

import com.google.gson.annotations.SerializedName;

public record StatusType(
        @SerializedName("IsOperational") Boolean isOperational,
        @SerializedName("IsUserSelectable") Boolean isUserSelectable,
        @SerializedName("ID") Integer id,
        @SerializedName("Title") String title
) {}