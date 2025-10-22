package com.kulenina.questix.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Alliance implements IIdentifiable, Serializable {
    public String id;
    public String name;
    public String leaderId; // Creator's user ID
    public List<String> memberIds; // All member user IDs (including leader)
    public boolean isMissionActive = false; // Whether a mission is currently running

    public int bossCurrentHp = 0;
    public int bossMaxHp = 0;
    public long missionStartedAt = 0;

    public long createdAt;
    public long updatedAt;

    public Alliance() {
        this.memberIds = new ArrayList<>();
        this.isMissionActive = false;
        this.bossCurrentHp = 0;
        this.bossMaxHp = 0;
        this.missionStartedAt = 0;
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

    public int getBossCurrentHp() {
        return bossCurrentHp;
    }

    public void setBossCurrentHp(int bossCurrentHp) {
        this.bossCurrentHp = bossCurrentHp;
        this.updatedAt = System.currentTimeMillis();
    }

    public int getBossMaxHp() {
        return bossMaxHp;
    }

    public void setBossMaxHp(int bossMaxHp) {
        this.bossMaxHp = bossMaxHp;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getMissionStartedAt() {
        return missionStartedAt;
    }
    public void setMissionStartedAt(long missionStartedAt) {
        this.missionStartedAt = missionStartedAt;
        this.updatedAt = System.currentTimeMillis();
    }
    public boolean isMissionActive() {
        return isMissionActive;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }
}
