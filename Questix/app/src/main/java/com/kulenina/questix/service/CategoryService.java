package com.kulenina.questix.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.kulenina.questix.model.Category;
import com.kulenina.questix.repository.CategoryRepository;

import java.util.List;
import java.util.UUID;

public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AppTaskService taskService;
    private final FirebaseAuth auth;

    public CategoryService() {
        this.categoryRepository = new CategoryRepository();
        this.taskService = new AppTaskService();
        this.auth = FirebaseAuth.getInstance();
    }

    private String getCurrentUserId() {
        if (auth.getCurrentUser() == null)
            throw new RuntimeException("The user is not logged in.");
        return auth.getCurrentUser().getUid();
    }

    public Task<String> createCategory(String name, String colorHex) {
        String userId = getCurrentUserId();

        return categoryRepository.findCategoriesByColor(colorHex, userId)
                .continueWithTask(task -> {
                    List<Category> existingCategories = task.getResult();
                    if (existingCategories != null && !existingCategories.isEmpty()) {
                        throw new RuntimeException("A category with the selected color already exists.");
                    }

                    String id = UUID.randomUUID().toString();
                    Category newCategory = new Category(id, name, colorHex, userId);

                    return categoryRepository.createWithId(newCategory)
                            .continueWith(createTask -> newCategory.getId());
                });
    }

    public Task<Void> updateCategoryColor(String categoryId, String newColorHex) {
        String userId = getCurrentUserId();

        return categoryRepository.findCategoriesByColor(newColorHex, userId)
                .continueWithTask(task -> {
                    List<Category> existingCategories = task.getResult();
                    boolean colorInUse = existingCategories.stream()
                            .anyMatch(c -> !c.getId().equals(categoryId));

                    if (colorInUse) {
                        throw new RuntimeException("A category with the selected color already exists.");
                    }

                    return categoryRepository.read(categoryId)
                            .continueWithTask(readTask -> {
                                Category categoryToUpdate = readTask.getResult();
                                if (categoryToUpdate == null || !userId.equals(categoryToUpdate.getUserId())) {
                                    throw new RuntimeException("You do not have permission to modify this category.");
                                }

                                categoryToUpdate.setColorHex(newColorHex);
                                Task<Void> updateTask = categoryRepository.update(categoryToUpdate);
                                Task<Void> updateTasksColor = taskService.updateTasksColorByCategory(categoryId, newColorHex);
                                return Tasks.whenAll(updateTask, updateTasksColor);
                            });
                });
    }
    public Task<Void> deleteCategory(String categoryId) {
        String userId = getCurrentUserId();

        return categoryRepository.read(categoryId)
                .continueWithTask(readTask -> {
                    Category category = readTask.getResult();
                    if (category == null || !userId.equals(category.getUserId())) {
                        throw new RuntimeException("You do not have permission to delete this category.");
                    }

                    return taskService.hasActiveTasksForCategory(categoryId)
                            .continueWithTask(task -> {
                                boolean hasActiveTasks = task.getResult();
                                if (hasActiveTasks) {
                                    throw new RuntimeException("The category cannot be deleted because it has active tasks assigned to it.");
                                }
                                return categoryRepository.delete(categoryId);
                            });
                });
    }
    public Task<List<Category>> getAllCategories() {
        String userId = getCurrentUserId();
        return categoryRepository.findAllByUser(userId);
    }

    public Task<Category> read(String categoryId) {
        String userId = getCurrentUserId();

        return categoryRepository.read(categoryId)
                .continueWith(task -> {
                    Category category = task.getResult();

                    if (category == null || !userId.equals(category.getUserId())) {
                        // Ako kategorija ne postoji ili ne pripada korisniku
                        // Važno: Možda ne želite da bacate izuzetak ovde, već da vratite null.
                        // Za LiveData posmatrače u ViewModelu je lakše vratiti null.
                        return null;
                    }
                    return category;
                });
    }
}
