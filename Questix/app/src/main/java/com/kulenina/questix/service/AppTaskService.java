package com.kulenina.questix.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth; // Dodao sam ga ovde jer ga CategoryService koristi
import com.kulenina.questix.model.Category;
import com.kulenina.questix.model.AppTask;
import com.kulenina.questix.model.QuotaState;
import com.kulenina.questix.model.User;
import com.kulenina.questix.repository.CategoryRepository;
import com.kulenina.questix.repository.AppTaskRepository;
import com.kulenina.questix.repository.UserRepository;
import com.kulenina.questix.repository.QuotaStateRepository; // NOVO
import com.kulenina.questix.service.TaskGeneratorService;

import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AppTaskService {

    private final AppTaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final LevelProgressionService levelService;
    private final QuotaStateRepository quotaRepository; // NOVO
    private final TaskGeneratorService taskGeneratorService; // NOVO

    private final FirebaseAuth auth; // Dodao

    // Fiksne XP Vrednosti po Specifikaciji 2.1
    private static final int XP_VERY_EASY = 1;
    private static final int XP_EASY = 3;
    private static final int XP_HARD = 7;
    private static final int XP_EXTREMELY_HARD = 20;
    private static final int XP_NORMAL = 1;
    private static final int XP_IMPORTANT = 3;
    private static final int XP_EXTREMELY_IMPORTANT = 10;
    private static final int XP_SPECIAL = 100;

    // Kvota po Specifikaciji 2.1
    private static final int QUOTA_VERY_EASY_NORMAL_DAILY = 5; // Veoma lak + Normalan
    private static final int QUOTA_EASY_IMPORTANT_DAILY = 5; // Lak + Važan
    private static final int QUOTA_HARD_EXTREMELY_IMPORTANT_DAILY = 2; // Težak + Ekstremno Važan
    private static final int QUOTA_EXTREMELY_HARD_WEEKLY = 1; // Ekstremno težak (Nije bitno + bilo koja bitnost)
    private static final int QUOTA_SPECIAL_MONTHLY = 1; // Specijalan (Bilo koja težina + Specijalan)

    // Ograničenje: Koliko dana unazad se može rešiti zadatak
    private static final long MAX_RESOLUTION_TIME_MILLIS = TimeUnit.DAYS.toMillis(3);
    private final AllianceMissionService missionService;

    public AppTaskService() {
        this.taskRepository = new AppTaskRepository();
        this.categoryRepository = new CategoryRepository();
        this.userRepository = new UserRepository();
        this.levelService = new LevelProgressionService();
        this.auth = FirebaseAuth.getInstance(); // Inicijalizacija
        this.quotaRepository = new QuotaStateRepository(); // NOVO
        this.taskGeneratorService = new TaskGeneratorService(); // NOVO
        this.missionService = new AllianceMissionService();
    }

    private String getCurrentUserId() {
        if (auth.getCurrentUser() == null)
            throw new RuntimeException("The user is not logged in.");
        return auth.getCurrentUser().getUid();
    }

    // --- Pomoćne metode za XP (FIKSNE vrednosti po 2.1) ---

    private int getDifficultyXp(String difficulty) {
        switch (difficulty) {
            case "Very Easy": return XP_VERY_EASY;
            case "Easy": return XP_EASY;
            case "Hard": return XP_HARD;
            case "Extremely Hard": return XP_EXTREMELY_HARD;
            default: throw new IllegalArgumentException("Invalid task difficulty: " + difficulty);
        }
    }

    private int getImportanceXp(String importance) {
        switch (importance) {
            case "Normal": return XP_NORMAL;
            case "Important": return XP_IMPORTANT;
            case "Extremely Important": return XP_EXTREMELY_IMPORTANT;
            case "Special": return XP_SPECIAL;
            default: throw new IllegalArgumentException("Invalid task importance: " + importance);
        }
    }

    // --- Pomoćne metode za CategoryService (koje ste već koristili) ---

    public Task<Boolean> hasActiveTasksForCategory(String categoryId) {
        return taskRepository.findTasksByCategoryAndStatus(categoryId, AppTask.STATUS_ACTIVE)
                .continueWith(task -> {
                    List<AppTask> activeTasks = task.getResult();
                    // Dodajemo i filtriranje po userId da budemo sigurni
                    String userId = getCurrentUserId();
                    return activeTasks != null && activeTasks.stream().anyMatch(t -> userId.equals(t.userId));
                });
    }

    public Task<Void> updateTasksColorByCategory(String categoryId, String newColorHex) {
        return taskRepository.findTasksByCategoryId(categoryId)
                .continueWithTask(task -> {
                    List<AppTask> tasksToUpdate = task.getResult();
                    String userId = getCurrentUserId();

                    // Filtrirajemo samo zadatke ulogovanog korisnika
                    List<AppTask> userTasksToUpdate = tasksToUpdate.stream()
                            .filter(t -> userId.equals(t.userId))
                            .collect(Collectors.toList());

                    if (userTasksToUpdate.isEmpty()) {
                        return Tasks.forResult(null);
                    }

                    return taskRepository.batchUpdateTaskColors(userTasksToUpdate, newColorHex);
                });
    }

    // --- 2.1. Kreiranje zadataka ---

    public Task<String> createTask(String userId, String categoryId, String name, String difficulty, String importance,
                                   boolean isRecurring, long executionTime, Integer repetitionInterval,
                                   String repetitionUnit, long startDate, long endDate, String description) {

        // Fiksno XP sabiranje (kao što je traženo u specifikaciji 2.1)
        int difficultyXp = getDifficultyXp(difficulty);
        int importanceXp = getImportanceXp(importance);
        int totalXp = difficultyXp + importanceXp;

        return categoryRepository.read(categoryId)
                .continueWithTask(task -> {
                    Category category = task.getResult();

                    if (category == null || !userId.equals(category.getUserId())) {
                        throw new RuntimeException("Category not found or access denied.");
                    }

                    AppTask newTask = new AppTask(userId, categoryId, category.getColorHex(), name, description, executionTime,
                            isRecurring, repetitionInterval, repetitionUnit, startDate, endDate,
                            difficulty, difficultyXp, importance, importanceXp);

                    newTask.totalXpValue = totalXp;

                    // Check if task execution time is more than 3 days in the past
                    long threeDaysAgo = System.currentTimeMillis() - MAX_RESOLUTION_TIME_MILLIS;
                    if (executionTime < threeDaysAgo) {
                        newTask.setStatus(AppTask.STATUS_UNDONE);
                    }

                    return taskRepository.createWithId(newTask)
                            .continueWith(createTask -> newTask.getId());
                });
    }

    // --- 2.2. Pregled zadataka (dopunjena logika) ---


    // --- 2.2. Pregled zadataka (korigovana logika za kalendar) ---

    public Task<List<AppTask>> getTaskOccurrencesByDateRange(String userId, long fromTime, long toTime) {

        return taskRepository.readAll()
                .continueWith(task -> {
                    List<AppTask> allTasks = task.getResult();
                    List<AppTask> userTasks = allTasks.stream()
                            .filter(t -> userId.equals(t.userId))
                            .collect(Collectors.toList());

                    List<AppTask> finalTaskList = new ArrayList<>();

                    for (AppTask t : userTasks) {
                        // Check if task is older than 3 days past execution time and still active - mark as undone
                        long now = System.currentTimeMillis();
                        long threeDaysAfterExecution = t.executionTime + MAX_RESOLUTION_TIME_MILLIS;
                        if (t.isActive() && now > threeDaysAfterExecution) {
                            t.setStatus(AppTask.STATUS_UNDONE);
                            // Update the task in database
                            taskRepository.update(t);
                        }
                        
                        if (t.isRecurring) {
                            // Generišemo sve instance u traženom opsegu
                            finalTaskList.addAll(taskGeneratorService.generateRecurringInstances(t, fromTime, toTime));
                        } else {
                            // Jednokratni zadaci: Dodajemo ih samo ako su u opsegu datuma
                            if (t.executionTime >= fromTime && t.executionTime <= toTime) {
                                finalTaskList.add(t);
                            }
                        }
                    }

                    // Uklanjamo duplikate
                    return finalTaskList.stream().distinct().collect(Collectors.toList());
                });
    }

    public Task<List<AppTask>> getTasksForList(String userId) {
        long now = System.currentTimeMillis();
        // Generišemo zadatke za period od 3 meseca unapred
        long toTime = now + TimeUnit.DAYS.toMillis(90);

        return taskRepository.readAll()
                .continueWith(task -> {
                    List<AppTask> allTasks = task.getResult();
                    List<AppTask> finalTaskList = new ArrayList<>();

                    // 1. Prikupljamo samo zadatke ulogovanog korisnika
                    List<AppTask> userTasks = allTasks.stream()
                            .filter(t -> userId.equals(t.userId))
                            .collect(Collectors.toList());

                    for (AppTask t : userTasks) {
                        // Check if task is older than 3 days past execution time and still active - mark as undone
                        long threeDaysAfterExecution = t.executionTime + MAX_RESOLUTION_TIME_MILLIS;
                        if (t.isActive() && now > threeDaysAfterExecution) {
                            t.setStatus(AppTask.STATUS_UNDONE);
                            // Update the task in database
                            taskRepository.update(t);
                        }
                        
                        if (t.isRecurring) {
                            // Generišemo instance (including past ones that might be undone)
                            finalTaskList.addAll(taskGeneratorService.generateRecurringInstances(t, now - TimeUnit.DAYS.toMillis(30), toTime));
                        } else {
                            // Jednokratni zadaci: Dodajemo aktivne i pauzirane zadatke
                            if (t.isActive() || t.isPaused()) {
                                finalTaskList.add(t);
                            }
                        }
                    }

                    // 2. Show active, paused, and canceled tasks (canceled only if execution time hasn't passed)
                    return finalTaskList.stream()
                            .filter(appTask -> {
                                System.out.println("DEBUG: Filtering task - ID: " + appTask.id + ", Status: " + appTask.status + ", ExecutionTime: " + appTask.executionTime);
                                
                                // Always show active and paused tasks
                                if (appTask.isActive() || appTask.isPaused()) {
                                    System.out.println("DEBUG: Task is active or paused - SHOWING");
                                    return true;
                                }
                                
                                // Show canceled tasks only if execution time hasn't passed yet
                                if (appTask.status.equals(AppTask.STATUS_CANCELED)) {
                                    long currentTime = System.currentTimeMillis();
                                    boolean shouldShow = appTask.executionTime >= currentTime;
                                    System.out.println("DEBUG: Canceled task - CurrentTime: " + currentTime + ", ShouldShow: " + shouldShow);
                                    return shouldShow;
                                }
                                
                                // Don't show other statuses (done, missed, undone)
                                System.out.println("DEBUG: Task has other status - HIDING");
                                return false;
                            })
                            .collect(Collectors.toList());
                });
    }

    // --- 2.3. Izmena i brisanje zadataka (Vaš kod je bio dobar, minimalno dopunjen) ---

    public Task<Void> deleteTask(String taskId) {
        String userId = getCurrentUserId();

        // Check if this is a recurring task instance (has timestamp suffix)
        final String originalTaskId;
        if (taskId.contains("_") && taskId.length() > 20) {
            // This is likely a generated instance ID, extract the original task ID
            int lastUnderscoreIndex = taskId.lastIndexOf("_");
            originalTaskId = taskId.substring(0, lastUnderscoreIndex);
        } else {
            originalTaskId = taskId;
        }

        return taskRepository.read(originalTaskId)
                .continueWithTask(task -> {
                    AppTask appTask = task.getResult();

                    if (appTask == null || !userId.equals(appTask.getUserId())) {
                        throw new RuntimeException("Task not found or permission denied.");
                    }

                    if (appTask.isFinished()) {
                        throw new RuntimeException("Cannot delete a finished task (done, missed, or canceled).");
                    }

                    if (appTask.isUndone()) {
                        throw new RuntimeException("Cannot delete an undone task.");
                    }

                    // Check if task is more than 3 days past execution time
                    long now = System.currentTimeMillis();
                    
                    // For recurring tasks, check the specific instance execution time
                    // For non-recurring tasks, check the task's execution time
                    long executionTimeToCheck;
                    if (appTask.isRecurring && taskId.contains("_") && taskId.length() > 20) {
                        // This is a recurring task instance, extract the execution time from the ID
                        try {
                            String[] parts = taskId.split("_");
                            if (parts.length == 2) {
                                executionTimeToCheck = Long.parseLong(parts[1]);
                            } else {
                                executionTimeToCheck = appTask.executionTime;
                            }
                        } catch (NumberFormatException e) {
                            executionTimeToCheck = appTask.executionTime;
                        }
                    } else {
                        // Non-recurring task or original recurring task
                        executionTimeToCheck = appTask.executionTime;
                    }
                    
                    long threeDaysAfterExecution = executionTimeToCheck + MAX_RESOLUTION_TIME_MILLIS;
                    if (now > threeDaysAfterExecution) {
                        throw new RuntimeException("Cannot delete tasks that are more than 3 days past their execution time.");
                    }

                    // Handle recurring tasks differently to preserve completed instances
                    if (appTask.isRecurring) {
                        // For recurring tasks, mark as canceled instead of deleting
                        // This stops future instance generation but preserves completed instances
                        appTask.setStatus(AppTask.STATUS_CANCELED);
                        return taskRepository.update(appTask);
                    } else {
                        // For non-recurring tasks, delete the specific task
                        return taskRepository.delete(originalTaskId);
                    }
                });
    }

    public Task<Void> updateTask(
            String taskId, String name, String description, long executionTime,
            String difficulty, String importance) {

        System.out.println("DEBUG: AppTaskService.updateTask() started");
        System.out.println("DEBUG: taskId: " + taskId);
        System.out.println("DEBUG: name: " + name);
        System.out.println("DEBUG: difficulty: " + difficulty);
        System.out.println("DEBUG: importance: " + importance);

        String userId = getCurrentUserId();
        System.out.println("DEBUG: userId: " + userId);

        // Check if this is a recurring task instance (has timestamp suffix)
        final String originalTaskId;
        if (taskId.contains("_") && taskId.length() > 20) {
            // This is likely a generated instance ID, extract the original task ID
            int lastUnderscoreIndex = taskId.lastIndexOf("_");
            originalTaskId = taskId.substring(0, lastUnderscoreIndex);
        } else {
            originalTaskId = taskId;
        }

        System.out.println("DEBUG: originalTaskId: " + originalTaskId);

        return taskRepository.read(originalTaskId)
                .continueWithTask(task -> {
                    System.out.println("DEBUG: Task read completed, success: " + task.isSuccessful());
                    AppTask appTask = task.getResult();

                    if (appTask == null || !userId.equals(appTask.getUserId())) {
                        System.out.println("DEBUG: Task not found or permission denied");
                        throw new RuntimeException("Task not found or permission denied.");
                    }

                    if (appTask.isFinished()) {
                        System.out.println("DEBUG: Task is finished");
                        throw new RuntimeException("Cannot modify a finished task (done, missed, or canceled).");
                    }

                    if (appTask.isUndone()) {
                        System.out.println("DEBUG: Task is undone");
                        throw new RuntimeException("Cannot modify an undone task.");
                    }

                    // Check if task is more than 3 days past execution time
                    long now = System.currentTimeMillis();
                    
                    // For recurring tasks, check the specific instance execution time
                    // For non-recurring tasks, check the task's execution time
                    long executionTimeToCheck;
                    if (appTask.isRecurring && taskId.contains("_") && taskId.length() > 20) {
                        // This is a recurring task instance, extract the execution time from the ID
                        try {
                            String[] parts = taskId.split("_");
                            if (parts.length == 2) {
                                executionTimeToCheck = Long.parseLong(parts[1]);
                            } else {
                                executionTimeToCheck = appTask.executionTime;
                            }
                        } catch (NumberFormatException e) {
                            executionTimeToCheck = appTask.executionTime;
                        }
                    } else {
                        // Non-recurring task or original recurring task
                        executionTimeToCheck = appTask.executionTime;
                    }
                    
                    long threeDaysAfterExecution = executionTimeToCheck + MAX_RESOLUTION_TIME_MILLIS;
                    if (now > threeDaysAfterExecution) {
                        System.out.println("DEBUG: Task instance is too old (execution time: " + executionTimeToCheck + ")");
                        throw new RuntimeException("Cannot modify tasks that are more than 3 days past their execution time.");
                    }

                    System.out.println("DEBUG: All validations passed, calculating XP");

                    // For recurring tasks, updating any instance updates the entire recurring task
                    // This means all future instances will have the new properties
                    int difficultyXp = getDifficultyXp(difficulty);
                    int importanceXp = getImportanceXp(importance);
                    int totalXp = difficultyXp + importanceXp;

                    System.out.println("DEBUG: XP calculated - difficulty: " + difficultyXp + ", importance: " + importanceXp + ", total: " + totalXp);

                    appTask.name = name;
                    appTask.description = description;
                    appTask.executionTime = executionTime;

                    appTask.difficulty = difficulty;
                    appTask.difficultyXp = difficultyXp;
                    appTask.importance = importance;
                    appTask.importanceXp = importanceXp;
                    appTask.totalXpValue = totalXp;
                    appTask.updatedAt = System.currentTimeMillis();

                    System.out.println("DEBUG: Task updated, calling repository.update()");
                    return taskRepository.update(appTask);
                });
    }

    // --- 2.4. Rešavanje zadataka (Potpuna Implementacija) ---

    public Task<Void> resolveTask(String taskId, String newStatus) {
        String userId = getCurrentUserId();

        // Check if this is a recurring task instance (has timestamp suffix)
        final String originalTaskId;
        if (taskId.contains("_") && taskId.length() > 20) {
            // This is likely a generated instance ID, extract the original task ID
            int lastUnderscoreIndex = taskId.lastIndexOf("_");
            originalTaskId = taskId.substring(0, lastUnderscoreIndex);
        } else {
            originalTaskId = taskId;
        }

        return taskRepository.read(originalTaskId)
                .continueWithTask(task -> {
                    AppTask appTask = task.getResult();

                    // Provera 1: Postojanje i autorizacija
                    if (appTask == null || !userId.equals(appTask.getUserId())) {
                        throw new RuntimeException("Task not found or permission denied.");
                    }

                    // Provera 2: Samo aktivan zadatak može se rešiti/pauzirati
                    if (!appTask.isActive()) {
                        throw new RuntimeException("Only active tasks can be resolved/paused.");
                    }

                    // Provera 2.5: Undone zadaci se ne mogu menjati
                    if (appTask.isUndone()) {
                        throw new RuntimeException("Cannot modify an undone task.");
                    }

                    // Provera 2.6: Tasks that are more than 3 days past execution time cannot be modified
                    long now = System.currentTimeMillis();
                    
                    // For recurring tasks, check the specific instance execution time
                    // For non-recurring tasks, check the task's execution time
                    long executionTimeToCheck;
                    if (appTask.isRecurring && taskId.contains("_") && taskId.length() > 20) {
                        // This is a recurring task instance, extract the execution time from the ID
                        try {
                            String[] parts = taskId.split("_");
                            if (parts.length == 2) {
                                executionTimeToCheck = Long.parseLong(parts[1]);
                            } else {
                                executionTimeToCheck = appTask.executionTime;
                            }
                        } catch (NumberFormatException e) {
                            executionTimeToCheck = appTask.executionTime;
                        }
                    } else {
                        // Non-recurring task or original recurring task
                        executionTimeToCheck = appTask.executionTime;
                    }
                    
                    long threeDaysAfterExecution = executionTimeToCheck + MAX_RESOLUTION_TIME_MILLIS;
                    if (now > threeDaysAfterExecution) {
                        throw new RuntimeException("Cannot modify tasks that are more than 3 days past their execution time.");
                    }

                    // Provera 3: Pauziranje
                    if (newStatus.equals(AppTask.STATUS_PAUSED) && !appTask.isRecurring) {
                        throw new RuntimeException("Only recurring tasks can be paused.");
                    }

                    // Provera 4: Vreme izvršenja (Ne može se uraditi zadatak u budućnosti)
                    if (newStatus.equals(AppTask.STATUS_DONE) && appTask.executionTime > System.currentTimeMillis()) {
                        throw new RuntimeException("Cannot mark future tasks as done.");
                    }

                    // Provera 5: Vreme izvršenja (Ne može se rešiti zadatak stariji od 3 dana)
                    if (newStatus.equals(AppTask.STATUS_DONE) || newStatus.equals(AppTask.STATUS_CANCELED)) {
                        long threeDaysAgo = System.currentTimeMillis() - MAX_RESOLUTION_TIME_MILLIS;
                        if (appTask.executionTime < threeDaysAgo) {
                            // Zadatak je previše star, već je trebao da pređe u "Undone"
                            appTask.setStatus(AppTask.STATUS_UNDONE);
                            return taskRepository.update(appTask)
                                    .continueWith(t -> {
                                        throw new RuntimeException("The task is too old and has been marked as undone.");
                                    });
                        }
                    }

                    // Ažuriranje statusa i snimanje
                    appTask.setStatus(newStatus);
                    Task<Void> updateTask = taskRepository.update(appTask);

                    // Dodeljivanje XP-a
                    if (newStatus.equals(AppTask.STATUS_DONE)) {
                        return handleXpAward(appTask).continueWithTask(xpTask -> {
                            // --- POZIV ZA SPECIJALNU MISIJU ---
                            return userRepository.read(appTask.userId).continueWithTask(userTask -> {
                                User user = userTask.getResult();
                                System.out.println("DEBUG: AppTaskService.resolveTask() - User: " + (user != null ? user.username : "null") + 
                                        ", isInAlliance: " + (user != null ? user.isInAlliance() : "N/A"));
                                
                                if (user != null && user.isInAlliance()) {
                                    String actionType = getMissionTaskActionType(appTask.difficulty, appTask.importance);
                                    System.out.println("DEBUG: AppTaskService.resolveTask() - Task difficulty: " + appTask.difficulty + 
                                            ", importance: " + appTask.importance + ", actionType: " + actionType);
                                    
                                    if (actionType != null) {
                                        System.out.println("DEBUG: AppTaskService.resolveTask() - Calling updateMissionProgress with actionType: " + actionType);
                                        return missionService.updateMissionProgress(user.currentAllianceId, appTask.userId, actionType)
                                                .continueWith(missionUpdateTask -> {
                                                    if (missionUpdateTask.isSuccessful() && missionUpdateTask.getResult()) {
                                                        System.out.println("DEBUG: AppTaskService.resolveTask() - Mission progress updated successfully for task completion");
                                                    } else {
                                                        System.out.println("DEBUG: AppTaskService.resolveTask() - Failed to update mission progress for task completion: " + 
                                                                (missionUpdateTask.getException() != null ? missionUpdateTask.getException().getMessage() : "Unknown error"));
                                                    }
                                                    return null;
                                                });
                                    } else {
                                        System.out.println("DEBUG: AppTaskService.resolveTask() - Task does not contribute to mission (actionType is null)");
                                    }
                                } else {
                                    System.out.println("DEBUG: AppTaskService.resolveTask() - User not in alliance or user is null");
                                }
                                return Tasks.forResult(null);
                            }).continueWithTask(missionTask -> updateTask);
                            // ---------------------------------
                        });
                    }

                    return updateTask;
                });
    }

    private Task<Void> handleXpAward(AppTask appTask) {
        String quotaType = getQuotaType(appTask.difficulty, appTask.importance);
        String periodUnit;
        int maxCount;

        switch (quotaType) {
            case "VE_N":
            case "E_I":
            case "H_EI":
                periodUnit = "Day";
                maxCount = (quotaType.equals("H_EI")) ? QUOTA_HARD_EXTREMELY_IMPORTANT_DAILY : QUOTA_VERY_EASY_NORMAL_DAILY;
                break;
            case "EH":
                periodUnit = "Week";
                maxCount = QUOTA_EXTREMELY_HARD_WEEKLY;
                break;
            case "S":
                periodUnit = "Month";
                maxCount = QUOTA_SPECIAL_MONTHLY;
                break;
            default:
                // Zadatak ne spada ni u jednu kvotu (npr. Lak + Normalan se ne kvotira)
                return Tasks.forResult(null);
        }

        long periodStart = getPeriodTimestamp(periodUnit);
        String quotaId = periodUnit.toLowerCase() + "_" + appTask.userId;

        // 1. Provera i povećanje kvote putem transakcije
        return quotaRepository.runQuotaTransaction(
                        new QuotaState(quotaId, appTask.userId, periodStart),
                        quotaType, maxCount)
                .continueWithTask(transactionTask -> {
                    QuotaState updatedState = transactionTask.getResult();

                    if (updatedState == null) {
                        // Transakcija je vratila null -> Kvota ispunjena. XP se ne dodeljuje.
                        return Tasks.forResult(null);
                    }

                    // 2. Ako je transakcija uspešna (kvota povećana), dodeli XP
                    return levelService.awardXpAndCheckLevelUp(appTask.userId, appTask.totalXpValue)
                            .continueWith(task -> null);
                });
    }

    private String getQuotaType(String difficulty, String importance) {
        if (difficulty.equals("Very Easy") && importance.equals("Normal")) {
            return "VE_N";
        }
        if (difficulty.equals("Easy") && importance.equals("Important")) {
            return "E_I";
        }
        if (difficulty.equals("Hard") && importance.equals("Extremely Important")) {
            return "H_EI";
        }
        // Ekstremno Težak se boduje bez obzira na bitnost (maks 1 slučaj nedeljno)
        if (difficulty.equals("Extremely Hard")) {
            return "EH";
        }
        // Specijalan se boduje bez obzira na težinu (maks 1 slučaj mesečno)
        if (importance.equals("Special")) {
            return "S";
        }
        return null; // Nema kvote
    }

    /**
     * Proverava sve aktivne zadatke starije od 3 dana i automatski ih označava kao Missed.
     */
    public Task<Void> checkAndMarkMissedTasks(String userId) {
        long threeDaysAgo = System.currentTimeMillis() - MAX_RESOLUTION_TIME_MILLIS;

        // Treba nam nova metoda u AppTaskRepository: findActiveTasksOlderThan()
        // Pretpostavljamo da taskRepository.readAll() vraća sve zadatke.

        return taskRepository.readAll()
                .continueWithTask(task -> {
                    List<AppTask> allTasks = task.getResult();
                    List<Task<Void>> updateTasks = new ArrayList<>();

                    if (allTasks == null) return Tasks.forResult(null);

                    List<AppTask> missedTasks = allTasks.stream()
                            .filter(t -> userId.equals(t.userId))
                            .filter(AppTask::isActive)
                            .filter(t -> t.executionTime < threeDaysAgo)
                            .collect(Collectors.toList());

                    for (AppTask t : missedTasks) {
                        t.setStatus(AppTask.STATUS_UNDONE);
                        updateTasks.add(taskRepository.update(t));
                        updateTasks.add(missionService.setMissedTaskFlag(userId));
                    }

                    return Tasks.whenAll(updateTasks);
                });
    }

    private long getPeriodTimestamp(String periodUnit) {
        Calendar cal = Calendar.getInstance();
        if ("Day".equals(periodUnit)) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } else if ("Week".equals(periodUnit)) {
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } else if ("Month".equals(periodUnit)) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        }
        return cal.getTimeInMillis();
    }
    public Task<AppTask> getTaskById(String taskId) {
        String userId = getCurrentUserId();

        // Handle generated recurring task IDs (format: "originalId_timestamp")
        String tempOriginalTaskId = taskId;
        long tempRequestedExecutionTime = 0;

        if (taskId.contains("_")) {
            String[] parts = taskId.split("_");
            if (parts.length == 2) {
                try {
                    tempOriginalTaskId = parts[0];
                    tempRequestedExecutionTime = Long.parseLong(parts[1]);
                } catch (NumberFormatException e) {
                    // If parsing fails, use the original taskId
                    tempOriginalTaskId = taskId;
                    tempRequestedExecutionTime = 0;
                }
            }
        }

        final String originalTaskId = tempOriginalTaskId;
        final long requestedExecutionTime = tempRequestedExecutionTime;

        return taskRepository.read(originalTaskId)
                .continueWith(task -> {
                    AppTask appTask = task.getResult();

                    if (appTask == null) {
                        throw new RuntimeException("Task not found or access denied.");
                    }

                    if (!userId.equals(appTask.getUserId())) {
                        throw new RuntimeException("Task not found or access denied.");
                    }

                    // If this is a generated recurring instance, update the execution time
                    if (requestedExecutionTime > 0 && appTask.isRecurring) {
                        AppTask instanceTask = cloneTaskForDisplay(appTask, requestedExecutionTime, taskId);
                        return instanceTask;
                    }

                    return appTask;
                });
    }

    /**
     * Creates a display copy of a recurring task with specific execution time
     */
    private AppTask cloneTaskForDisplay(AppTask originalTask, long executionTime, String displayId) {
        AppTask instance = new AppTask(
                originalTask.userId, originalTask.categoryId, originalTask.colorHex,
                originalTask.name, originalTask.description, executionTime,
                originalTask.isRecurring, originalTask.repetitionInterval, originalTask.repetitionUnit,
                originalTask.startDate, originalTask.endDate,
                originalTask.difficulty, originalTask.difficultyXp, originalTask.importance, originalTask.importanceXp
        );

        // Set the display ID and status
        instance.id = displayId;
        // Inherit status from original task for all instances
        instance.status = originalTask.status;
        instance.totalXpValue = originalTask.totalXpValue;

        return instance;
    }

    private String getMissionTaskActionType(String difficulty, String importance) {
        // Rešavanje veoma lakog, lakog, normalnog ili važnog zadatka (max 10) - 1 HP
        if ((difficulty.equals("Very Easy") || difficulty.equals("Easy")) &&
                (importance.equals("Normal") || importance.equals("Important"))) {

            // Specijalni slučaj: ako je Lak I Normalan, računa se 2 puta
            if (difficulty.equals("Easy") && importance.equals("Normal")) {
                return "LIGHT_TASK_DOUBLE";
            }
            return "LIGHT_TASK";
        }

        // Rešavanje ostalih zadataka (max 6) - 4 HP
        if (difficulty.equals("Hard") || difficulty.equals("Extremely Hard") ||
                importance.equals("Extremely Important") || importance.equals("Special")) {
            // Paziti: Lak + Važan se kvotira, ali nije "Ostali" zadatak.
            // Pretpostavljamo da "Ostali" obuhvata Težak, Ekstremno Težak, Ekstremno Važan i Specijalan.

            // Laki zadaci su već obrađeni iznad. Fokusiramo se na T/ET/EV/S.
            if (difficulty.equals("Hard") || difficulty.equals("Extremely Hard") ||
                    importance.equals("Extremely Important") || importance.equals("Special")) {
                return "HEAVY_TASK";
            }
        }
        return null; // Zadatak ne doprinosi misiji
    }

}