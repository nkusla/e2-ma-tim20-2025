package com.kulenina.questix.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth; // Dodao sam ga ovde jer ga CategoryService koristi
import com.kulenina.questix.model.Category;
import com.kulenina.questix.model.AppTask;
import com.kulenina.questix.model.QuotaState;
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

    public AppTaskService() {
        this.taskRepository = new AppTaskRepository();
        this.categoryRepository = new CategoryRepository();
        this.userRepository = new UserRepository();
        this.levelService = new LevelProgressionService();
        this.auth = FirebaseAuth.getInstance(); // Inicijalizacija
        this.quotaRepository = new QuotaStateRepository(); // NOVO
        this.taskGeneratorService = new TaskGeneratorService(); // NOVO
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
                        if (t.isRecurring) {
                            // Generišemo samo BUDUĆE instance
                            finalTaskList.addAll(taskGeneratorService.generateRecurringInstances(t, now, toTime));
                        } else {
                            // Jednokratni zadaci: Samo trenutni i budući
                            if (t.executionTime >= now) {
                                finalTaskList.add(t);
                            }
                        }
                    }

                    // 2. Filtriramo one koji su već završeni (done, canceled, missed)
                    // (Iako bi generator trebao generisati samo aktivne, ovo je sigurnosna provera)
                    return finalTaskList.stream()
                            .filter(AppTask::isActive)
                            .collect(Collectors.toList());
                });
    }

    // --- 2.3. Izmena i brisanje zadataka (Vaš kod je bio dobar, minimalno dopunjen) ---

    public Task<Void> deleteTask(String taskId) {
        String userId = getCurrentUserId();

        return taskRepository.read(taskId)
                .continueWithTask(task -> {
                    AppTask appTask = task.getResult();

                    if (appTask == null || !userId.equals(appTask.getUserId())) {
                        throw new RuntimeException("Task not found or permission denied.");
                    }

                    if (appTask.isFinished()) {
                        throw new RuntimeException("Cannot delete a finished task (done, missed, or canceled).");
                    }

                    // Brisanje kompletne instance (što se tretira kao brisanje svih ponavljanja)
                    return taskRepository.delete(taskId);
                });
    }

    public Task<Void> updateTask(
            String taskId, String name, String description, long executionTime,
            String difficulty, String importance) {

        String userId = getCurrentUserId();

        return taskRepository.read(taskId)
                .continueWithTask(task -> {
                    AppTask appTask = task.getResult();

                    if (appTask == null || !userId.equals(appTask.getUserId())) {
                        throw new RuntimeException("Task not found or permission denied.");
                    }

                    if (appTask.isFinished()) {
                        throw new RuntimeException("Cannot modify a finished task (done, missed, or canceled).");
                    }

                    // Izmena ponavljajućeg zadatka: Ako je executionTime u prošlosti, smatramo da korisnik menja buduća ponavljanja

                    int difficultyXp = getDifficultyXp(difficulty);
                    int importanceXp = getImportanceXp(importance);
                    int totalXp = difficultyXp + importanceXp;

                    appTask.name = name;
                    appTask.description = description;
                    appTask.executionTime = executionTime;

                    appTask.difficulty = difficulty;
                    appTask.difficultyXp = difficultyXp;
                    appTask.importance = importance;
                    appTask.importanceXp = importanceXp;
                    appTask.totalXpValue = totalXp;
                    appTask.updatedAt = System.currentTimeMillis();

                    return taskRepository.update(appTask);
                });
    }

    // --- 2.4. Rešavanje zadataka (Potpuna Implementacija) ---

    public Task<Void> resolveTask(String taskId, String newStatus) {
        String userId = getCurrentUserId();

        return taskRepository.read(taskId)
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
                            // Zadatak je previše star, već je trebao da pređe u "Missed"
                            appTask.setStatus(AppTask.STATUS_MISSED);
                            return taskRepository.update(appTask)
                                    .continueWith(t -> {
                                        throw new RuntimeException("The task is too old and has been marked as missed.");
                                    });
                        }
                    }

                    // Ažuriranje statusa i snimanje
                    appTask.setStatus(newStatus);
                    Task<Void> updateTask = taskRepository.update(appTask);

                    // Dodeljivanje XP-a
                    if (newStatus.equals(AppTask.STATUS_DONE)) {
                        return handleXpAward(appTask).continueWithTask(xpTask -> updateTask);
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
                        t.setStatus(AppTask.STATUS_MISSED);
                        updateTasks.add(taskRepository.update(t));
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

        return taskRepository.read(taskId)
                .continueWith(task -> {
                    AppTask appTask = task.getResult();

                    if (appTask == null || !userId.equals(appTask.getUserId())) {
                        // Ako zadatak nije pronađen ili pripada drugom korisniku
                        throw new RuntimeException("Task not found or access denied.");
                    }
                    return appTask;
                });
    }

}