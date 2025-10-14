package com.kulenina.questix.viewmodel;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import com.google.android.gms.tasks.Task;
import com.kulenina.questix.BR; // Uvozimo BR klasu za Data Binding
import com.kulenina.questix.model.Category;
import com.kulenina.questix.service.CategoryService;
import java.util.Collections;
import java.util.List;

public class CategoryViewModel extends BaseObservable {

    private final CategoryService categoryService;
    private List<Category> categories;
    private String errorMessage;
    private boolean isLoading;

    public CategoryViewModel() {
        this.categoryService = new CategoryService();
        this.categories = Collections.emptyList();
        this.isLoading = false;
        this.errorMessage = null;
        loadCategories();
    }

    @Bindable
    public boolean getIsLoading() {
        return isLoading;
    }

    public void setIsLoading(boolean loading) {
        this.isLoading = loading;
        notifyPropertyChanged(BR.isLoading);
    }

    @Bindable
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String message) {
        this.errorMessage = message;
        notifyPropertyChanged(BR.errorMessage);
    }
    @Bindable
    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
        notifyPropertyChanged(BR.categories);
    }

    public void loadCategories() {
        setIsLoading(true);
        setErrorMessage(null);

        categoryService.getAllCategories()
                .addOnCompleteListener(task -> {
                    setIsLoading(false);
                    if (task.isSuccessful()) {
                        setCategories(task.getResult());
                    } else {
                        setErrorMessage(task.getException().getMessage());
                    }
                });
    }

    public void createCategory(String name, String colorHex) {
        setIsLoading(true);
        setErrorMessage(null);

        categoryService.createCategory(name, colorHex)
                .addOnCompleteListener(task -> {
                    setIsLoading(false);
                    if (task.isSuccessful()) {
                        loadCategories(); // Osveži listu nakon uspešnog kreiranja
                    } else {
                        setErrorMessage(task.getException().getMessage());
                    }
                });
    }

    public void deleteCategory(String categoryId) {
        setIsLoading(true);
        setErrorMessage(null);

        categoryService.deleteCategory(categoryId)
                .addOnCompleteListener(task -> {
                    setIsLoading(false);
                    if (task.isSuccessful()) {
                        loadCategories(); // Osveži listu nakon brisanja
                    } else {
                        setErrorMessage(task.getException().getMessage());
                    }
                });
    }

    public void updateCategoryColor(String categoryId, String newColorHex) {
        setIsLoading(true);
        setErrorMessage(null);

        categoryService.updateCategoryColor(categoryId, newColorHex)
                .addOnCompleteListener(task -> {
                    setIsLoading(false);
                    if (task.isSuccessful()) {
                        loadCategories(); // Osveži listu nakon izmene
                    } else {
                        setErrorMessage(task.getException().getMessage());
                    }
                });
    }

    public void clearErrorMessage() {
        setErrorMessage(null);
    }
}