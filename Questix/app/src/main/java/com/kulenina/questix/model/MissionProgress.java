package com.kulenina.questix.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class MissionProgress implements IIdentifiable {

    public String id; // missionId_userId
    public String allianceId;
    public String userId;

    public int purchasesCount = 0; // Kupovina u prodavnici (max 5) - 2 HP
    public int successfulHitsCount = 0; // Uspešan udarac u regularnoj borbi (max 10) - 2 HP
    public int lightTasksCount = 0; // Rešavanje VL, L, N, V zadataka (max 10) - 1 HP (L+N = 2 puta)
    public int heavyTasksCount = 0; // Rešavanje ostalih zadataka (max 6) - 4 HP

    public boolean hasMissedTask = false; // Bez nerešenih zadataka tokom misije - 10 HP

    // Set datuma (kao string npr. "2025-10-21") za praćenje poslatih poruka (max 4 HP dnevno)
    public Set<String> messageDays = new HashSet<>();

    public int totalHpContribution = 0; // Ukupno smanjenje HP bosa od strane ovog korisnika

    @ServerTimestamp
    public Date createdAt;

    public MissionProgress() {}

    public MissionProgress(String allianceId, String userId) {
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
        return messageDays.size();
    }
}