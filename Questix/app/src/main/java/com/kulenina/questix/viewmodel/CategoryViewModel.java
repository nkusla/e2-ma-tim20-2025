package com.kulenina.questix.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.kulenina.questix.model.Category;
import com.kulenina.questix.service.CategoryService;

import java.util.List;
import java.util.Collections;

// NASLEĐUJE ViewModeL, ŠTO JE POTREBNO ZA ViewModelProvider
public class CategoryViewModel extends ViewModel {

    private final CategoryService categoryService;

    // MutableLiveData za podatke koje Fragment posmatra
    private final MutableLiveData<List<Category>> _categories = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);

    // LiveData za posmatranje iz Fragmenta
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
                        // Koristimo postValue jer se može pozvati sa background threada
                        _errorMessage.postValue(task.getException().getMessage());
                    }
                });
    }

    // KORIGOVANA METODA U CategoryViewModel.java
    public Task<Void> createCategory(String name, String colorHex) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        // Vraćamo Task<Void> da bi se poklopilo sa potpisom,
        // i dodajemo onSuccessListener da bismo loadCategories pozvali tek nakon uspeha
        return categoryService.createCategory(name, colorHex)
                .continueWithTask(task -> {
                    _isLoading.postValue(false);
                    if (task.isSuccessful()) {
                        // Ako je uspešno, pokrećemo novo učitavanje
                        loadCategories();
                        // Vraćamo uspešan Task<Void> (prazan Task)
                        return com.google.android.gms.tasks.Tasks.forResult(null);
                    } else {
                        // Ako nije uspešno, postavljamo grešku i vraćamo neuspešan Task<Void>
                        _errorMessage.postValue(task.getException().getMessage());
                        throw task.getException();
                    }
                })
                .continueWith(task -> {
                    // Ovaj deo osigurava da Task<Void> bude vraćen čak i ako je došlo do greške
                    if (!task.isSuccessful()) {
                        // Greška je već zabeležena iznad
                        throw task.getException();
                    }
                    return null; // Vraća Task<Void>
                });
    }

    public Task<Void> deleteCategory(String categoryId) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        return categoryService.deleteCategory(categoryId)
                .addOnCompleteListener(task -> {
                    _isLoading.postValue(false);
                    if (task.isSuccessful()) {
                        loadCategories(); // Osveži listu nakon brisanja
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
                        loadCategories(); // Osveži listu nakon izmene
                    } else {
                        _errorMessage.postValue(task.getException().getMessage());
                    }
                });
    }

    public void clearErrorMessage() {
        _errorMessage.setValue(null);
    }

    public LiveData<Category> getCategoryById(String categoryId) {
        // Koristimo MutableLiveData koji će biti vraćen i ažuriran asinhrono
        MutableLiveData<Category> liveData = new MutableLiveData<>();

        if (categoryId == null || categoryId.isEmpty()) {
            liveData.setValue(null);
            return liveData;
        }

        // Pretpostavljamo da categoryService ima getCategoryById(String)
        categoryService.read(categoryId) // Koristim 'read' jer je to standardni Repository metod
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Postavljanje rezultata (Category objekat)
                        liveData.postValue(task.getResult());
                    } else {
                        // Postavljanje null i greške (opcionalno)
                        _errorMessage.postValue("Failed to load category: " + task.getException().getMessage());
                        liveData.postValue(null);
                    }
                });

        return liveData;
    }
}