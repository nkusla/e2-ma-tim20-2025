package com.kulenina.questix.model;

import java.io.Serializable;

public class AllianceMessage implements IIdentifiable, Serializable {
    public String id;
    public String allianceId;
    public String senderId;
    public String senderUsername; // For display purposes
    public String message;
    public long timestamp;
    public String messageType; // "text", "system" (for system messages like "User joined")

    public AllianceMessage() {
        this.timestamp = System.currentTimeMillis();
        this.messageType = "text";
    }

    public AllianceMessage(String id, String allianceId, String senderId,
                          String senderUsername, String message) {
        this();
        this.id = id;
        this.allianceId = allianceId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.message = message;
    }

    public AllianceMessage(String id, String allianceId, String message) {
        this();
        this.id = id;
        this.allianceId = allianceId;
        this.senderId = "system";
        this.senderUsername = "System";
        this.message = message;
        this.messageType = "system";
    }

    @Override
    public String getId() {
        return id;
    }

    public boolean isSystemMessage() {
        return "system".equals(messageType);
    }

    public boolean isTextMessage() {
        return "text".equals(messageType);
    }
}
