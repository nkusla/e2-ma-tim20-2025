package com.kulenina.questix.model;

import java.io.Serializable;
import java.util.UUID;

public class AppTask implements IIdentifiable, Serializable {
    public String id;
    public String name;
    public String description;
    public String userId;
    public String categoryId;
    public String colorHex;

    public boolean isRecurring;
    public long executionTime;
    public Integer repetitionInterval; // 1, 2, 3...
    public String repetitionUnit; // "Day", "Week"
    public long startDate;
    public long endDate;

    // Difficulty (XP values)
    public String difficulty; // "Very Easy", "Easy", "Hard", "Extremely Hard"
    public int difficultyXp;

    public String importance; // "Normal", "Important", "Extremely Important", "Special"
    public int importanceXp;

    public int totalXpValue; // difficultyXp + importanceXp

    // Statuses: active, done, missed, paused (only for recurring), canceled
    public String status;
    public long completedAt; // Timestamp when marked as done/missed/canceled

    public long createdAt;
    public long updatedAt;

    // --- Static Constants for Statuses and Units ---
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_DONE = "done";
    public static final String STATUS_UNDONE = "undone";
    public static final String STATUS_MISSED = "missed";
    public static final String STATUS_PAUSED = "paused";
    public static final String STATUS_CANCELED = "canceled";

    public static final String UNIT_DAY = "Day";
    public static final String UNIT_WEEK = "Week";

    public AppTask() {
    }

    public AppTask(String userId, String categoryId, String colorHex, String name, String description, long executionTime,
                boolean isRecurring, Integer repetitionInterval, String repetitionUnit, long startDate, long endDate,
                String difficulty, int difficultyXp, String importance, int importanceXp) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.categoryId = categoryId;
        this.colorHex = colorHex;
        this.name = name;
        this.description = description;
        this.executionTime = executionTime;

        this.isRecurring = isRecurring;
        this.repetitionInterval = repetitionInterval;
        this.repetitionUnit = repetitionUnit;
        this.startDate = startDate;
        this.endDate = endDate;

        this.difficulty = difficulty;
        this.difficultyXp = difficultyXp;
        this.importance = importance;
        this.importanceXp = importanceXp;
        this.totalXpValue = difficultyXp + importanceXp;

        this.status = STATUS_ACTIVE;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    @Override
    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
        if (status.equals(STATUS_DONE) || status.equals(STATUS_MISSED) || status.equals(STATUS_CANCELED) || status.equals(STATUS_UNDONE)) {
            this.completedAt = System.currentTimeMillis();
        }
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status) || STATUS_PAUSED.equals(status);
    }

    public boolean isPaused() {
        return STATUS_PAUSED.equals(status);
    }

    public boolean isUndone() {
        return STATUS_UNDONE.equals(status);
    }

    public boolean isDone() {
        return STATUS_DONE.equals(status);
    }

    public boolean isFinished() {
        return STATUS_DONE.equals(status) || STATUS_MISSED.equals(status) || 
               STATUS_CANCELED.equals(status) || STATUS_UNDONE.equals(status);
    }
}