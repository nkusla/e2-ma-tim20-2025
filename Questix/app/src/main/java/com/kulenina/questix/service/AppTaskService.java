package com.kulenina.questix.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.kulenina.questix.model.Category;
import com.kulenina.questix.model.AppTask;
import com.kulenina.questix.repository.CategoryRepository;
import com.kulenina.questix.repository.AppTaskRepository;
import com.kulenina.questix.repository.UserRepository;

import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class AppTaskService {

    private final AppTaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final LevelProgressionService levelService;

    private static final int QUOTA_VERY_EASY_NORMAL_DAILY = 5;
    private static final int QUOTA_EASY_IMPORTANT_DAILY = 5;
    private static final int QUOTA_HARD_EXTREMELY_IMPORTANT_DAILY = 2;
    private static final int QUOTA_EXTREMELY_HARD_WEEKLY = 1;
    private static final int QUOTA_SPECIAL_MONTHLY = 1;

    public AppTaskService() {
        this.taskRepository = new AppTaskRepository();
        this.categoryRepository = new CategoryRepository();
        this.userRepository = new UserRepository();
        this.levelService = new LevelProgressionService();
    }


    public Task<Boolean> hasActiveTasksForCategory(String categoryId) {
        return taskRepository.findTasksByCategoryAndStatus(categoryId, AppTask.STATUS_ACTIVE)
                .continueWith(task -> {
                    List<AppTask> activeTasks = task.getResult();
                    return activeTasks != null && !activeTasks.isEmpty();
                });
    }

    public Task<Void> updateTasksColorByCategory(String categoryId, String newColorHex) {
        return taskRepository.findTasksByCategoryId(categoryId)
                .continueWithTask(task -> {
                    List<AppTask> tasksToUpdate = task.getResult();
                    if (tasksToUpdate == null || tasksToUpdate.isEmpty()) {
                        return Tasks.forResult(null);
                    }

                    return taskRepository.batchUpdateTaskColors(tasksToUpdate, newColorHex);
                });
    }


    public Task<String> createTask(String userId, String categoryId, String name, String difficulty, String importance,
                                   boolean isRecurring, long executionTime, Integer repetitionInterval,
                                   String repetitionUnit, long startDate, long endDate, String description) {

        return Tasks.whenAllSuccess(
                categoryRepository.read(categoryId),
                levelService.getCurrentLevel(userId)
        ).continueWithTask(task -> {
            List<Object> results = task.getResult();
            Category category = (Category) results.get(0);
            Integer userLevel = (Integer) results.get(1);

            if (category == null) {
                throw new RuntimeException("Category not found.");
            }

            int totalXp = levelService.calculateDynamicXpReward(userLevel, importance, difficulty);

            int finalDifficultyXp = totalXp;
            int finalImportanceXp = 0;

            AppTask newTask = new AppTask(userId, categoryId, category.getColorHex(), name, description, executionTime,
                    isRecurring, repetitionInterval, repetitionUnit, startDate, endDate,
                    difficulty, finalDifficultyXp, importance, finalImportanceXp);

            newTask.totalXpValue = totalXp;

            // 3. Snimanje u bazu
            return taskRepository.createWithId(newTask)
                    .continueWith(createTask -> newTask.getId());
        });
    }

    public Task<List<AppTask>> getTasksForCalendar(String userId) {
        return taskRepository.readAll();
    }

    public Task<List<AppTask>> getTasksForList(String userId) {
        return taskRepository.readAll()
                .continueWith(task -> {
                    long now = System.currentTimeMillis();
                    List<AppTask> allTasks = task.getResult();
                    List<AppTask> futureTasks = new ArrayList<>();

                    for (AppTask t : allTasks) {
                        if (t.executionTime >= now) {
                            futureTasks.add(t);
                        }
                    }
                    return futureTasks;
                });
    }

    public Task<Void> resolveTask(String taskId, String newStatus) {
        return taskRepository.read(taskId)
                .continueWithTask(task -> {
                    AppTask appTask = task.getResult();

                    if (appTask == null) {
                        throw new RuntimeException("Task not found.");
                    }

                    if (!appTask.isActive()) {
                        throw new RuntimeException("Only active tasks can be resolved/paused.");
                    }

                    if (newStatus.equals(AppTask.STATUS_PAUSED) && !appTask.isRecurring) {
                        throw new RuntimeException("Only recurring tasks can be paused.");
                    }

                    if (newStatus.equals(AppTask.STATUS_DONE) && appTask.executionTime > System.currentTimeMillis()) {
                        throw new RuntimeException("Cannot mark future tasks as done.");
                    }

                    if (newStatus.equals(AppTask.STATUS_DONE) || newStatus.equals(AppTask.STATUS_CANCELED)) {
                        long threeDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3);
                        if (appTask.executionTime < threeDaysAgo) {
                            throw new RuntimeException("Cannot resolve tasks older than 3 days. They are automatically marked as missed.");
                        }
                    }

                    appTask.setStatus(newStatus);
                    Task<Void> updateTask = taskRepository.update(appTask);

                    if (newStatus.equals(AppTask.STATUS_DONE)) {
                        return handleXpAward(appTask).continueWithTask(xpTask -> updateTask);
                    }

                    return updateTask;
                });
    }

    private Task<Void> handleXpAward(AppTask appTask) {
        if (!isTaskEligibleForXpQuota(appTask.difficulty, appTask.importance)) {
        }

        return levelService.awardXpAndCheckLevelUp(appTask.userId, appTask.totalXpValue)
                .continueWith(task -> null);
    }

    private boolean isTaskEligibleForXpQuota(String difficulty, String importance) {
        return true;
    }

    public Task<Void> checkAndMarkMissedTasks(String userId) {
        long threeDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3);

        List<AppTask> missedTasks = new ArrayList<>();

        List<Task<Void>> updateTasks = new ArrayList<>();

        for (AppTask task : missedTasks) {
            task.setStatus(AppTask.STATUS_MISSED);
            updateTasks.add(taskRepository.update(task));
        }

        return Tasks.whenAll(updateTasks);
    }
}