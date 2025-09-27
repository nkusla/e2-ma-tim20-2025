package com.kulenina.questix.model;

import java.io.Serializable;

public class AllianceInvitation implements IIdentifiable, Serializable {
    public String id;
    public String allianceId;
    public String allianceName; // For display purposes
    public String inviterId; // Who sent the invitation
    public String inviterUsername; // For display purposes
    public String inviteeId; // Who received the invitation
    public String status; // "pending", "accepted", "declined"
    public long createdAt;
    public long updatedAt;

    public AllianceInvitation() {
        this.status = "pending";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public AllianceInvitation(String id, String allianceId, String allianceName,
                             String inviterId, String inviterUsername, String inviteeId) {
        this();
        this.id = id;
        this.allianceId = allianceId;
        this.allianceName = allianceName;
        this.inviterId = inviterId;
        this.inviterUsername = inviterUsername;
        this.inviteeId = inviteeId;
    }

    @Override
    public String getId() {
        return id;
    }

    public boolean isPending() {
        return "pending".equals(status);
    }

    public boolean isAccepted() {
        return "accepted".equals(status);
    }

    public boolean isDeclined() {
        return "declined".equals(status);
    }

    public void accept() {
        this.status = "accepted";
        this.updatedAt = System.currentTimeMillis();
    }

    public void decline() {
        this.status = "declined";
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean canBeRespondedTo() {
        return isPending();
    }
}
