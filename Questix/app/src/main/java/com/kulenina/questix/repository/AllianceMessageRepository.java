package com.kulenina.questix.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.kulenina.questix.model.AllianceMessage;
import com.kulenina.questix.mapper.Mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class AllianceMessageRepository extends Repository<AllianceMessage> {
    private final FirebaseFirestore db;
    private final Mapper<AllianceMessage> mapper;

    public AllianceMessageRepository() {
        super("alliance_messages", AllianceMessage.class);
        this.db = FirebaseFirestore.getInstance();
        this.mapper = new Mapper<>(AllianceMessage.class);
    }

    public Task<List<AllianceMessage>> getMessagesByAllianceId(String allianceId) {
        return db.collection("alliance_messages")
            .whereEqualTo("allianceId", allianceId)
            .get()
            .continueWith(task -> {
                QuerySnapshot querySnapshot = task.getResult();
                List<AllianceMessage> messages = new ArrayList<>();
                if (querySnapshot != null) {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        AllianceMessage message = toObject(document);
                        if (message != null) {
                            messages.add(message);
                        }
                    }
                }

                // Sort messages by timestamp in ascending order (oldest first)
                Collections.sort(messages, new Comparator<AllianceMessage>() {
                    @Override
                    public int compare(AllianceMessage m1, AllianceMessage m2) {
                        return Long.compare(m1.timestamp, m2.timestamp);
                    }
                });

                return messages;
            });
    }

    private AllianceMessage toObject(DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }

        String documentId = document.getId();
        Map<String, Object> data = document.getData();
        return mapper.fromMap(data, documentId);
    }
}
