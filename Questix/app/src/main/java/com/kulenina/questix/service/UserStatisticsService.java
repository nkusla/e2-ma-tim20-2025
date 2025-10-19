package com.kulenina.questix.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.kulenina.questix.model.AppTask;
import com.kulenina.questix.model.Category;
import com.kulenina.questix.model.StatisticsData;
import com.kulenina.questix.model.User;
import com.kulenina.questix.repository.AppTaskRepository;
import com.kulenina.questix.repository.CategoryRepository;
import com.kulenina.questix.repository.UserRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UserStatisticsService {
    private final AppTaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final FirebaseAuth auth;

    public UserStatisticsService() {
        this.taskRepository = new AppTaskRepository();
        this.categoryRepository = new CategoryRepository();
        this.userRepository = new UserRepository();
        this.auth = FirebaseAuth.getInstance();
    }

    public Task<StatisticsData> getUserStatistics() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Tasks.forException(new RuntimeException("User not logged in"));
        }

        return taskRepository.findAllByUser(userId)
            .continueWithTask(task -> {
                List<AppTask> taskResult = task.getResult();
                final List<AppTask> allTasks = taskResult != null ? taskResult : new ArrayList<>();

                return categoryRepository.findAllByUser(userId)
                    .continueWith(categoryTask -> {
                        List<Category> categoryResult = categoryTask.getResult();
                        final List<Category> categories = categoryResult != null ? categoryResult : new ArrayList<>();

                        return calculateStatistics(allTasks, categories);
                    });
            });
    }

    private StatisticsData calculateStatistics(List<AppTask> allTasks, List<Category> categories) {
        StatisticsData stats = new StatisticsData();

        // Calculate active days count (days with at least one completed task)
        stats.activeDaysCount = calculateActiveDays(allTasks);

        // Calculate task counts
        stats.totalCreatedTasks = allTasks.size();
        stats.totalCompletedTasks = (int) allTasks.stream()
            .filter(task -> AppTask.STATUS_DONE.equals(task.status))
            .count();
        stats.totalMissedTasks = (int) allTasks.stream()
            .filter(task -> AppTask.STATUS_MISSED.equals(task.status))
            .count();
        stats.totalCanceledTasks = (int) allTasks.stream()
            .filter(task -> AppTask.STATUS_CANCELED.equals(task.status))
            .count();

        // Calculate longest success streak
        stats.longestSuccessStreak = calculateLongestStreak(allTasks);

        // Calculate completed tasks by category
        stats.completedTasksByCategory = calculateTasksByCategory(allTasks, categories);

        // Calculate average difficulty XP data
        stats.averageDifficultyData = calculateAverageDifficultyData(allTasks);

        // Calculate last 7 days XP progress
        stats.last7DaysXp = calculateLast7DaysXp(allTasks);

        // Calculate special missions
        calculateSpecialMissions(allTasks, stats);

        return stats;
    }

    private int calculateActiveDays(List<AppTask> allTasks) {
        Map<String, Boolean> activeDays = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (AppTask task : allTasks) {
            if (AppTask.STATUS_DONE.equals(task.status) && task.completedAt > 0) {
                String dateKey = dateFormat.format(new Date(task.completedAt));
                activeDays.put(dateKey, true);
            }
        }

        return activeDays.size();
    }

    private int calculateLongestStreak(List<AppTask> allTasks) {
        // Group tasks by day and check for consecutive days with completed tasks
        Map<String, Boolean> dayHasCompletedTask = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (AppTask task : allTasks) {
            if (AppTask.STATUS_DONE.equals(task.status) && task.completedAt > 0) {
                String dateKey = dateFormat.format(new Date(task.completedAt));
                dayHasCompletedTask.put(dateKey, true);
            }
        }

        // Find longest consecutive streak
        int longestStreak = 0;
        int currentStreak = 0;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        // Check backwards from today
        for (int i = 0; i < 365; i++) { // Check last year
            String dateKey = dateFormat.format(calendar.getTime());

            if (dayHasCompletedTask.containsKey(dateKey)) {
                currentStreak++;
                longestStreak = Math.max(longestStreak, currentStreak);
            } else {
                // Check if there are any tasks for this day (active or missed)
                boolean hasTasksForDay = allTasks.stream()
                    .anyMatch(task -> {
                        String taskDate = dateFormat.format(new Date(task.executionTime));
                        return taskDate.equals(dateKey);
                    });

                // Only break streak if there were tasks but none completed
                if (hasTasksForDay) {
                    currentStreak = 0;
                }
                // If no tasks for the day, streak continues
            }

            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }

        return longestStreak;
    }

    private Map<String, Integer> calculateTasksByCategory(List<AppTask> allTasks, List<Category> categories) {
        Map<String, Integer> categoryMap = new HashMap<>();
        Map<String, String> categoryIdToName = new HashMap<>();

        // Create mapping from category ID to name
        for (Category category : categories) {
            categoryIdToName.put(category.getId(), category.name);
        }

        // Count completed tasks by category
        for (AppTask task : allTasks) {
            if (AppTask.STATUS_DONE.equals(task.status)) {
                String categoryName = categoryIdToName.getOrDefault(task.categoryId, "Unknown");
                categoryMap.put(categoryName, categoryMap.getOrDefault(categoryName, 0) + 1);
            }
        }

        return categoryMap;
    }

    private List<StatisticsData.DifficultyXpData> calculateAverageDifficultyData(List<AppTask> allTasks) {
        Map<String, List<Integer>> difficultyXpMap = new HashMap<>();

        for (AppTask task : allTasks) {
            if (AppTask.STATUS_DONE.equals(task.status)) {
                difficultyXpMap.computeIfAbsent(task.difficulty, k -> new ArrayList<>())
                    .add(task.totalXpValue);
            }
        }

        List<StatisticsData.DifficultyXpData> result = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entry : difficultyXpMap.entrySet()) {
            List<Integer> xpValues = entry.getValue();
            double average = xpValues.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            result.add(new StatisticsData.DifficultyXpData(entry.getKey(), average, xpValues.size()));
        }

        return result;
    }

    private List<StatisticsData.XpProgressData> calculateLast7DaysXp(List<AppTask> allTasks) {
        List<StatisticsData.XpProgressData> result = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        for (int i = 6; i >= 0; i--) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, -i);

            String dateKey = dateFormat.format(calendar.getTime());
            long dayStart = calendar.getTimeInMillis();
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            long dayEnd = calendar.getTimeInMillis();

            int dayXp = allTasks.stream()
                .filter(task -> AppTask.STATUS_DONE.equals(task.status))
                .filter(task -> task.completedAt >= dayStart && task.completedAt < dayEnd)
                .mapToInt(task -> task.totalXpValue)
                .sum();

            result.add(new StatisticsData.XpProgressData(dateKey, dayXp));
        }

        return result;
    }

    private void calculateSpecialMissions(List<AppTask> allTasks, StatisticsData stats) {
        // Special missions are tasks with "Special" importance
        stats.startedSpecialMissions = (int) allTasks.stream()
            .filter(task -> "Special".equals(task.importance))
            .count();

        stats.completedSpecialMissions = (int) allTasks.stream()
            .filter(task -> "Special".equals(task.importance))
            .filter(task -> AppTask.STATUS_DONE.equals(task.status))
            .count();
    }

    private String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
}
