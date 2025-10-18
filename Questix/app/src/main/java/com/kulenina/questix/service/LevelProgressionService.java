package com.kulenina.questix.service;

import com.google.android.gms.tasks.Task;
import com.kulenina.questix.model.User;
import com.kulenina.questix.repository.UserRepository;

public class LevelProgressionService {
    private final UserRepository userRepository;

    public LevelProgressionService() {
        this.userRepository = new UserRepository();
    }

    public Task<User> awardXpAndCheckLevelUp(String userId, int xpAmount) {
        return userRepository.read(userId)
            .continueWithTask(task -> {
                User user = task.getResult();
                if (user == null) {
                    throw new RuntimeException("User not found");
                }

                user.xp += xpAmount;

                while (canLevelUp(user)) {
                    levelUpUser(user);
                }

                return userRepository.update(user)
                    .continueWith(updateTask -> user);
            });
    }

    public int getXpRequiredForLevel(int level) {
        if (level <= 1) return 200;

        int previousLevelXp = getXpRequiredForLevel(level - 1);
        float nextLevelXp = previousLevelXp * 2 + previousLevelXp / 2.0f;

        return Math.round(nextLevelXp);
    }

    public int calculateDynamicXpReward(int userLevel, String importance, String difficulty) {
        int baseImportanceXp = getBaseImportanceXp(importance);
        int baseDifficultyXp = getBaseDifficultyXp(difficulty);

        for (int i = 2; i <= userLevel; i++) {
            baseImportanceXp = Math.round(baseImportanceXp + (baseImportanceXp / 2.0f));
            baseDifficultyXp = Math.round(baseDifficultyXp + (baseDifficultyXp / 2.0f));
        }

        return baseImportanceXp + baseDifficultyXp;
    }

    private boolean canLevelUp(User user) {
        int totalXpForNextLevel = getXpRequiredForLevel(user.level + 1);
        return user.xp >= totalXpForNextLevel;
    }

    private void levelUpUser(User user) {
        user.level++;

        int ppReward = calculatePowerPointReward(user.level);
        user.powerPoints += ppReward;
    }

    private int calculatePowerPointReward(int level) {
        if (level <= 1) return 0;
        if (level == 2) return 40;

        int previousPP = calculatePowerPointReward(level - 1);
        return Math.round(previousPP + (3 * previousPP / 4.0f));
    }

    private int getBaseImportanceXp(String importance) {
        if (importance == null) return 15;
        switch (importance.toLowerCase()) {
            case "low": return 10;
            case "medium": return 20;
            case "high": return 30;
            default: return 15;
        }
    }

    private int getBaseDifficultyXp(String difficulty) {
        if (difficulty == null) return 10;
        switch (difficulty.toLowerCase()) {
            case "easy": return 5;
            case "medium": return 15;
            case "hard": return 25;
            default: return 10;
        }
    }

    public Task<Integer> getCurrentLevel(String userId) {
        return userRepository.read(userId)
                .continueWith(task -> {
                    User user = task.getResult();
                    if (user == null) {
                        throw new RuntimeException("User not found");
                    }
                    return user.level;
                });
    }
}
