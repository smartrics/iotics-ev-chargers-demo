package com.iotics.demo.ocm;

import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.time.ZonedDateTime;

public record DataProvider(
        @SerializedName("WebsiteURL") String websiteURL,
        @SerializedName("Comments") String comments,
        @SerializedName("DataProviderStatusType") DataProviderStatusType dataProviderStatusType,
        @SerializedName("IsRestrictedEdit") Boolean isRestrictedEdit,
        @SerializedName("IsOpenDataLicensed") Boolean isOpenDataLicensed,
        @SerializedName("IsApprovedImport") Boolean isApprovedImport,
        @SerializedName("License") String license,
        @SerializedName("DateLastImported") ZonedDateTime dateLastImported,
        @SerializedName("ID") Integer id,
        @SerializedName("Title") String title
) {}