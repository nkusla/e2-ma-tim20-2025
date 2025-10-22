package com.kulenina.questix.model;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MissionProgress implements IIdentifiable, Serializable {

    public String id; // missionId_userId
    public String allianceId;
    public String userId;

    public int purchasesCount = 0; // Kupovina u prodavnici (max 5) - 2 HP
    public int successfulHitsCount = 0; // Uspešan udarac u regularnoj borbi (max 10) - 2 HP
    public int lightTasksCount = 0; // Rešavanje VL, L, N, V zadataka (max 10) - 1 HP (L+N = 2 puta)
    public int heavyTasksCount = 0; // Rešavanje ostalih zadataka (max 6) - 4 HP

    public boolean hasMissedTask = false; // Bez nerešenih zadataka tokom misije - 10 HP

    // List datuma (kao string npr. "2025-10-21") za praćenje poslatih poruka (max 4 HP dnevno)
    public List<String> messageDays;

    public int totalHpContribution = 0; // Ukupno smanjenje HP bosa od strane ovog korisnika

    public long createdAt;

    public MissionProgress() {
        this.messageDays = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public MissionProgress(String allianceId, String userId) {
        this();
        this.id = allianceId + "_" + userId;
        this.allianceId = allianceId;
        this.userId = userId;
    }

    @Override
    public String getId() {
        return id;
    }

    @Exclude
    public int getMessageDaysCount() {
        return getMessageDays().size();
    }
    
    @Exclude
    public List<String> getMessageDays() {
        if (messageDays == null) {
            messageDays = new ArrayList<>();
        }
        return messageDays;
    }
}