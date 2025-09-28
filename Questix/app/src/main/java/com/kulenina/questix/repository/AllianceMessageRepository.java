package com.kulenina.questix.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.Query;
import com.kulenina.questix.model.AllianceMessage;

import java.util.List;

public class AllianceMessageRepository extends Repository<AllianceMessage> {

    public AllianceMessageRepository() {
        super("alliance_messages", AllianceMessage.class);
    }

    public Task<List<AllianceMessage>> getMessagesByAllianceId(String allianceId) {
        return getCollectionReference()
            .whereEqualTo("allianceId", allianceId)
            .get()
            .continueWith(task -> {
                List<AllianceMessage> messages = toList(task.getResult());
                messages.sort((m1, m2) -> Long.compare(m1.timestamp, m2.timestamp));
                return messages;
            });
    }

}
