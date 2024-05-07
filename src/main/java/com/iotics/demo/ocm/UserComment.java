package com.iotics.demo.ocm;

import com.google.gson.annotations.SerializedName;

import java.net.URL;
import java.time.ZonedDateTime;

public record UserComment (
        @SerializedName("ID") Integer id,
        @SerializedName("ChargePointID") Integer chargePointID,
        @SerializedName("CommentTypeID") Integer commentTypeID,
        @SerializedName("CommentType") CommentType commentType,
        @SerializedName("UserName") String userName,
        @SerializedName("Comment") String comment,
        @SerializedName("Rating") Integer rating,
        @SerializedName("RelatedURL") URL relatedURL,
        @SerializedName("DateCreated") ZonedDateTime dateCreated,
        @SerializedName("User") User user,
        @SerializedName("CheckinStatusTypeID") Integer checkinStatusTypeID,
        @SerializedName("CheckinStatusType") CheckinStatusType checkinStatusType,
        @SerializedName("IsActionedByEditor") Boolean isActionedByEditor
) {}


