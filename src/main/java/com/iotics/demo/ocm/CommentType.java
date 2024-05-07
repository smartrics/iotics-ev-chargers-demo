package com.iotics.demo.ocm;

import com.google.gson.annotations.SerializedName;

public record CommentType(
        @SerializedName("ID") Integer id,
        @SerializedName("Title") String title
) {
}
