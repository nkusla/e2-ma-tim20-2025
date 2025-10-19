package com.kulenina.questix.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.Query;
import com.kulenina.questix.model.MissionProgress;

import java.util.List;

public class MissionProgressRepository extends Repository<MissionProgress> {

    // Koristi se konstruktor kao u AppTaskRepository: super(String, Class<T>)
    public MissionProgressRepository() {
        super("missionProgresses", MissionProgress.class);
    }

    protected String getCollectionName() {
        return "missionProgresses";
    }

    /**
     * Dobavlja sve MissionProgress dokumente za dati allianceId.
     */
    public Task<List<MissionProgress>> getAllProgressesByAllianceId(String allianceId) {
        Query query = getCollectionReference().whereEqualTo("allianceId", allianceId);

        return query.get().continueWith(task -> toList(task.getResult()));
    }
}