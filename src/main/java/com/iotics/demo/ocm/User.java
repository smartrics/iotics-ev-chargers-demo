package com.iotics.demo.ocm;

import com.google.gson.annotations.SerializedName;

import java.net.URL;

public record User(
        @SerializedName("ID") Integer id,
        @SerializedName("IdentityProvider") String identityProvider,
        @SerializedName("Identifier") String identifier,
        @SerializedName("CurrentSessionToken") String currentSessionToken,
        @SerializedName("Username") String username,
        @SerializedName("Profile") Object profile,
        @SerializedName("Location") Object location,
        @SerializedName("WebsiteURL") URL websiteURL,
        @SerializedName("ReputationPoints") Integer reputationPoints,
        @SerializedName("Permissions") Object permissions,
        @SerializedName("PermissionsRequested") Object permissionsRequested,
        @SerializedName("DateCreated") Object dateCreated,
        @SerializedName("DateLastLogin") Object dateLastLogin,
        @SerializedName("IsProfilePublic") Boolean isProfilePublic,
        @SerializedName("IsEmergencyChargingProvider") Boolean isEmergencyChargingProvider,
        @SerializedName("IsPublicChargingProvider") Boolean isPublicChargingProvider,
        @SerializedName("Latitude") Double latitude,
        @SerializedName("Longitude") Double longitude,
        @SerializedName("EmailAddress") String emailAddress,
        @SerializedName("EmailHash") String emailHash,
        @SerializedName("ProfileImageURL") URL profileImageURL,
        @SerializedName("IsCurrentSessionTokenValid") Boolean isCurrentSessionTokenValid,
        @SerializedName("APIKey") String apiKey,
        @SerializedName("SyncedSettings") Object syncedSettings
) {
}
