package com.kulenina.questix.model;

import java.io.Serializable;

public class QuotaState implements IIdentifiable, Serializable {
    public String id; // Koristimo fiksni ID tipa "daily_user_ID" ili "monthly_user_ID"
    public String userId;
    public long timestamp; // Početak perioda (npr. ponoć za dnevnu kvotu)

    // Dnevne kvote
    public int veryEasyNormalCount = 0; // Max 5
    public int easyImportantCount = 0;   // Max 5
    public int hardExtremelyImportantCount = 0; // Max 2

    // Nedeljne kvote
    public int extremelyHardCount = 0; // Max 1

    // Mesečne kvote
    public int specialCount = 0; // Max 1

    public QuotaState() {
    }

    public QuotaState(String id, String userId, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    @Override
    public String getId() {
        return id;
    }
}