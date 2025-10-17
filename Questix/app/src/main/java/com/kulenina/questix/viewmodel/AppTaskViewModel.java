package com.kulenina.questix.viewmodel;

import com.google.android.gms.tasks.Task;
import com.kulenina.questix.model.AppTask;
import com.kulenina.questix.service.AppTaskService;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel; // DODATNO: Neophodan uvoz za ViewModelProvider

// KLJUČNA KOREKCIJA: Nasleđivanje androidx.lifecycle.ViewModel
public class AppTaskViewModel extends ViewModel {

    // ... Za sada ne koristimo AndroidX Lifecycle ViewModel, ali ga možemo koristiti sa LiveData za Observable pattern.

    private final AppTaskService taskService;
    private final FirebaseAuth auth;

    private final MutableLiveData<AppTask> _taskDetails = new MutableLiveData<>();

    // LiveData za zadatke u Kalendaru (koji su preračunata ponavljanja)
    private final MutableLiveData<List<AppTask>> _tasksForSelectedDate = new MutableLiveData<>();

    // LiveData za zadatke u glavnoj listi (nepreračunati zadaci/ponavljanja)
    private final MutableLiveData<List<AppTask>> _listTasks = new MutableLiveData<>();

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);

    // Korigovani nazivi za LiveData (koristimo LiveData<T> kao povratni tip, MutableLiveData<T> interno)
    public LiveData<List<AppTask>> getTasksForSelectedDate() { return _tasksForSelectedDate; }
    public LiveData<List<AppTask>> getTasksForList() { return _listTasks; }
    public LiveData<String> getErrorLiveData() { return _error; } // Korigovano ime da se ne kosi sa getError()
    public LiveData<Boolean> isLoading() { return _isLoading; }

    // Metoda koja je nedostajala u prethodnom kodu za CalendarFragment
    public void clearError() {
        _error.setValue(null);
    }

    // Korigovan konstruktor
    public AppTaskViewModel() {
        this.taskService = new AppTaskService();
        this.auth = FirebaseAuth.getInstance();
    }

    private String getCurrentUserId() {
        if (auth.getCurrentUser() == null) {
            _error.postValue("User not authenticated.");
            throw new IllegalStateException("User not logged in.");
        }
        return auth.getCurrentUser().getUid();
    }

    // --- 2.2. Pregled zadataka ---

    // Ova metoda iz Kalendar Fragmenta (prethodno nazvana loadTasksForCalendar)
    public void loadTaskOccurrencesByDateRange(long dateStartInMillis, long dateEndInMillis) {
        String userId = getCurrentUserId();
        _isLoading.setValue(true);
        // Pretpostavljamo da taskService ima metodu koja preračunava ponavljanja
        taskService.getTaskOccurrencesByDateRange(userId, dateStartInMillis, dateEndInMillis)
                .addOnCompleteListener(task -> {
                    _isLoading.postValue(false);
                    if (task.isSuccessful()) {
                        _tasksForSelectedDate.postValue(task.getResult());
                    } else {
                        _error.postValue("Failed to load calendar tasks: " + task.getException().getMessage());
                    }
                });
    }

    public void loadTasksForList() {
        String userId = getCurrentUserId();
        _isLoading.setValue(true);
        // Ova metoda bi trebalo da učita listu za glavni TaskListFragment
        taskService.getTasksForList(userId)
                .addOnCompleteListener(task -> {
                    _isLoading.postValue(false);
                    if (task.isSuccessful()) {
                        _listTasks.postValue(task.getResult());
                    } else {
                        _error.postValue("Failed to load list tasks: " + task.getException().getMessage());
                    }
                });
    }

    // --- 2.1. Kreiranje zadataka ---

    public Task<String> createTask(
            String categoryId, String name, String difficulty, String importance,
            boolean isRecurring, long executionTime, Integer repetitionInterval,
            String repetitionUnit, long startDate, long endDate, String description) {

        String userId = getCurrentUserId();

        _isLoading.setValue(true);
        return taskService.createTask(userId, categoryId, name, difficulty, importance,
                        isRecurring, executionTime, repetitionInterval,
                        repetitionUnit, startDate, endDate, description)
                .addOnCompleteListener(task -> {
                    _isLoading.postValue(false);
                    if (task.isSuccessful()) {
                        // Ponovo učitaj liste nakon kreiranja
                        loadTasksForList();
                        // Nema potrebe za loadTaskOccurrencesByDateRange, jer će to pokrenuti CalendarFragment
                    } else {
                        _error.postValue("Task creation failed: " + task.getException().getMessage());
                    }
                });
    }

    // --- 2.3. Izmena i brisanje zadataka ---

    public Task<Void> deleteTask(String taskId) {
        _isLoading.setValue(true);
        return taskService.deleteTask(taskId)
                .addOnCompleteListener(task -> {
                    _isLoading.postValue(false);
                    if (task.isSuccessful()) {
                        loadTasksForList();
                    } else {
                        _error.postValue("Task deletion failed: " + task.getException().getMessage());
                    }
                });
    }

    public Task<Void> updateTask(
            String taskId, String name, String description, long executionTime,
            String difficulty, String importance) {

        _isLoading.setValue(true);
        return taskService.updateTask(taskId, name, description, executionTime, difficulty, importance)
                .addOnCompleteListener(task -> {
                    _isLoading.postValue(false);
                    if (task.isSuccessful()) {
                        loadTasksForList();
                    } else {
                        _error.postValue("Task update failed: " + task.getException().getMessage());
                    }
                });
    }

    // --- 2.4. Rešavanje zadataka ---

    public Task<Void> resolveTask(String taskId, String newStatus) {
        _isLoading.setValue(true);
        return taskService.resolveTask(taskId, newStatus)
                .addOnCompleteListener(task -> {
                    _isLoading.postValue(false);
                    if (task.isSuccessful()) {
                        loadTasksForList();
                    } else {
                        _error.postValue("Task resolution failed: " + task.getException().getMessage());
                    }
                });
    }

    // Dodatna sistemska provera (za background service/worker)
    public Task<Void> checkMissedTasks() {
        String userId = getCurrentUserId();
        return taskService.checkAndMarkMissedTasks(userId);
    }

    public LiveData<AppTask> getTaskDetails() {
        return _taskDetails;
    }

    public void loadTaskDetails(String taskId) {
        _isLoading.setValue(true);
        taskService.getTaskById(taskId)
                .addOnCompleteListener(task -> {
                    _isLoading.postValue(false);
                    if (task.isSuccessful()) {
                        AppTask appTask = task.getResult();
                        _taskDetails.postValue(appTask);
                    } else {
                        // KORIGOVANO: Koristimo _error polje
                        _error.postValue(task.getException().getMessage());
                        _taskDetails.postValue(null);
                    }
                });
    }
}