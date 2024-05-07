package com.iotics.demo.ocm;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

public record Connection(@SerializedName("ID") Integer id, @SerializedName("ConnectionTypeID") Integer connectionTypeID,
                         @SerializedName("ConnectionType") ConnectionType connectionType,
                         @SerializedName("Reference") String reference,
                         @SerializedName("StatusTypeID") Integer statusTypeID,
                         @SerializedName("StatusType") StatusType statusType,
                         @SerializedName("LevelID") Integer levelID, @SerializedName("Level") Level level,
                         @SerializedName("Amps") Integer amps, @SerializedName("Voltage") Integer voltage,
                         @SerializedName("PowerKW") Integer powerKW,
                         @SerializedName("CurrentTypeID") Integer currentTypeID,
                         @SerializedName("CurrentType") CurrentType currentType,
                         @SerializedName("Quantity") Integer quantity, @SerializedName("Comments") String comments) {

    @NotNull
    public String makeLabel() {
        return this.connectionType().formalName() + ",ID:" + this.id();
    }

    public String makeComment(String extra) {
        String twinComment = "Unavailable for " + makeLabel();
        try {
            twinComment = makeLabel() + "@" + extra;
        } catch (Exception ex) {
            // ignore
        }
        return twinComment;
    }

    @NotNull
    public String makeUserSelectableStatus() {
        StatusType statusType = this.statusType();
        String selectable;
        if (statusType == null || statusType.isUserSelectable() == null) {
            selectable = "false";
        } else {
            selectable = statusType.isUserSelectable().toString();
        }
        return selectable;
    }

    @NotNull
    public String makeOperationalStatus() {
        StatusType statusType = this.statusType();
        String isOperational;
        if (statusType == null || statusType.isOperational() == null) {
            isOperational = "false";
        } else {
            isOperational = statusType.isOperational().toString();
        }
        return isOperational;
    }

}
