package com.iotics.demo.ocm;

import com.google.gson.annotations.SerializedName;

public record SubmissionStatus(
        @SerializedName("IsLive") Boolean isLive,
        @SerializedName("ID") Integer id,
        @SerializedName("Title") String title
) {}
