package com.kulenina.questix.repository;

import com.google.android.gms.tasks.Task;
import com.kulenina.questix.model.Category;
import java.util.List;

public class CategoryRepository extends Repository<Category> {

    private static final String COLLECTION_NAME = "categories";

    public CategoryRepository() {
        super(COLLECTION_NAME, Category.class);
    }
    public Task<List<Category>> findAllByUser(String userId) {
        return getCollectionReference()
                .whereEqualTo("userId", userId)
                .get()
                .continueWith(task -> toList(task.getResult()));
    }

    public Task<List<Category>> findCategoriesByColor(String colorHex, String userId) {
        return getCollectionReference()
                .whereEqualTo("colorHex", colorHex)
                .whereEqualTo("userId", userId)
                .get()
                .continueWith(task -> toList(task.getResult()));
    }
}
