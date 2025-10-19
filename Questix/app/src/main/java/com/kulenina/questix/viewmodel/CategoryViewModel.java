package com.kulenina.questix.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.kulenina.questix.model.Category;
import com.kulenina.questix.service.CategoryService;

import java.util.List;
import java.util.Collections;

public class CategoryViewModel extends ViewModel {

    private final CategoryService categoryService;

    private final MutableLiveData<List<Category>> _categories = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);

    public LiveData<List<Category>> getCategories() { return _categories; }
    public LiveData<String> getErrorMessage() { return _errorMessage; }
    public LiveData<Boolean> isLoading() { return _isLoading; }

    public CategoryViewModel() {
        this.categoryService = new CategoryService();
        loadCategories();
    }

    public void loadCategories() {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        categoryService.getAllCategories()
                .addOnCompleteListener(task -> {
                    _isLoading.postValue(false);
                    if (task.isSuccessful()) {
                        _categories.postValue(task.getResult());
                    } else {
                        _errorMessage.postValue(task.getException().getMessage());
                    }
                });
    }

    public Task<Void> createCategory(String name, String colorHex) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        return categoryService.createCategory(name, colorHex)
                .continueWithTask(task -> {
                    _isLoading.postValue(false);
                    if (task.isSuccessful()) {
                        loadCategories();
                        return com.google.android.gms.tasks.Tasks.forResult(null);
                    } else {
                        _errorMessage.postValue(task.getException().getMessage());
                        throw task.getException();
                    }
                })
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return null;
                });
    }

    public Task<Void> deleteCategory(String categoryId) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        return categoryService.deleteCategory(categoryId)
                .addOnCompleteListener(task -> {
                    _isLoading.postValue(false);
                    if (task.isSuccessful()) {
                        loadCategories();
                    } else {
                        _errorMessage.postValue(task.getException().getMessage());
                    }
                });
    }

    public Task<Void> updateCategoryColor(String categoryId, String newColorHex) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        return categoryService.updateCategoryColor(categoryId, newColorHex)
                .addOnCompleteListener(task -> {
                    _isLoading.postValue(false);
                    if (task.isSuccessful()) {
                        loadCategories();
                    } else {
                        _errorMessage.postValue(task.getException().getMessage());
                    }
                });
    }

    public void clearErrorMessage() {
        _errorMessage.setValue(null);
    }

    public LiveData<Category> getCategoryById(String categoryId) {
        MutableLiveData<Category> liveData = new MutableLiveData<>();

        if (categoryId == null || categoryId.isEmpty()) {
            liveData.setValue(null);
            return liveData;
        }

        categoryService.read(categoryId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        liveData.postValue(task.getResult());
                    } else {
                        _errorMessage.postValue("Failed to load category: " + task.getException().getMessage());
                        liveData.postValue(null);
                    }
                });

        return liveData;
    }
}