package com.kulenina.questix.model;

import java.io.Serializable;
import java.util.UUID;

public class Category implements IIdentifiable, Serializable {
    public String id;
    public String name;
    public String colorHex;
    public String userId;
    public long createdAt;
    public long updatedAt;

    public Category() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Category(String id, String name, String colorHex, String userId) {
        this.id = id;
        this.name = name;
        this.colorHex = colorHex;
        this.userId = userId;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Category(String name, String colorHex, String userId) {
        this(UUID.randomUUID().toString(), name, colorHex, userId);
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
