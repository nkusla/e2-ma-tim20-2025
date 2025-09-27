package com.kulenina.questix.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Alliance implements IIdentifiable, Serializable {
    public String id;
    public String name;
    public String leaderId; // Creator's user ID
    public List<String> memberIds; // All member user IDs (including leader)
    public boolean isMissionActive; // Whether a mission is currently running
    public long createdAt;
    public long updatedAt;

    public Alliance() {
        this.memberIds = new ArrayList<>();
        this.isMissionActive = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Alliance(String id, String name, String leaderId) {
        this();
        this.id = id;
        this.name = name;
        this.leaderId = leaderId;
        // Leader is automatically added as first member
        this.memberIds.add(leaderId);
    }

    @Override
    public String getId() {
        return id;
    }

    public void addMember(String userId) {
        if (memberIds == null) {
            memberIds = new ArrayList<>();
        }
        if (!memberIds.contains(userId)) {
            memberIds.add(userId);
            updatedAt = System.currentTimeMillis();
        }
    }

    public void removeMember(String userId) {
        if (memberIds != null) {
            memberIds.remove(userId);
            updatedAt = System.currentTimeMillis();
        }
    }

    public boolean isMember(String userId) {
        return memberIds != null && memberIds.contains(userId);
    }

    public boolean isLeader(String userId) {
        return leaderId != null && leaderId.equals(userId);
    }

    public int getMemberCount() {
        return memberIds != null ? memberIds.size() : 0;
    }

    public boolean canLeave(String userId) {
        // Leader cannot leave, and members cannot leave during active mission
        return !isLeader(userId) && !isMissionActive;
    }

    public boolean canDisband(String userId) {
        // Only leader can disband, and only if no mission is active
        return isLeader(userId) && !isMissionActive;
    }

    public void setMissionActive(boolean missionActive) {
        this.isMissionActive = missionActive;
        this.updatedAt = System.currentTimeMillis();
    }
}
