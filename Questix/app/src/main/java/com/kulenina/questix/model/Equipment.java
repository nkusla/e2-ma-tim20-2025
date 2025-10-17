package com.kulenina.questix.model;

import java.io.Serializable;

public abstract class Equipment implements IIdentifiable, Serializable {
    public String id;
    public String userId;
    public String name;
    public EquipmentType type;
    public boolean isActive;
    public boolean isExpired;

    public enum EquipmentType {
        POTION, CLOTHING, WEAPON
    }

    public Equipment() {
        this.isActive = false;
        this.isExpired = false;
    }

    public Equipment(String userId, String name, EquipmentType type) {
        this();
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.isExpired = false;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EquipmentType getType() {
        return type;
    }

    public void setType(EquipmentType type) {
        this.type = type;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
        this.isExpired = false;
    }

    public boolean isExpired() {
        return isExpired;
    }

    public void setExpired(boolean expired) {
        this.isExpired = expired;
    }

    public abstract int getPrice(int baseBossReward);
    public abstract String getEffectDescription();
    public abstract void useBattle();
}
