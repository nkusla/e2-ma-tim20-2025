package com.kulenina.questix.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.kulenina.questix.model.AppTask;
import java.util.List;

public class AppTaskRepository extends Repository<AppTask> {

    public AppTaskRepository() {
        super("tasks", AppTask.class);
    }

    public Task<List<AppTask>> findTasksByCategoryAndStatus(String categoryId, String status) {
        return getCollectionReference()
                .whereEqualTo("categoryId", categoryId)
                .whereEqualTo("status", status)
                .get()
                .continueWith(t -> toList(t.getResult()));
    }

    public Task<List<AppTask>> findTasksByCategoryId(String categoryId) {
        return getCollectionReference()
                .whereEqualTo("categoryId", categoryId)
                .get()
                .continueWith(t -> toList(t.getResult()));
    }

    public Task<Void> batchUpdateTaskColors(List<AppTask> tasksToUpdate, String newColorHex) {
        if (tasksToUpdate.isEmpty()) {
            return com.google.android.gms.tasks.Tasks.forResult(null);
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();
        CollectionReference tasksRef = getCollectionReference();

        for (AppTask appTask : tasksToUpdate) {
            batch.update(tasksRef.document(appTask.getId()), "colorHex", newColorHex);
        }

        return batch.commit();
    }
}