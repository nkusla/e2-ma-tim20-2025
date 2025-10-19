package com.kulenina.questix.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.kulenina.questix.model.*;
import com.kulenina.questix.repository.UserRepository;
import com.kulenina.questix.repository.BossBattleRepository;

import java.util.Random;

public class BossBattleService {
    private final UserRepository userRepository;
    private final BossBattleRepository bossBattleRepository;
    private final EquipmentService equipmentService;
    private final LevelProgressionService levelProgressionService;
    private final FirebaseAuth auth;
    private final Random random;

    public BossBattleService() {
        this.userRepository = new UserRepository();
        this.bossBattleRepository = new BossBattleRepository();
        this.equipmentService = new EquipmentService();
        this.levelProgressionService = new LevelProgressionService();
        this.auth = FirebaseAuth.getInstance();
        this.random = new Random();
    }

    private String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    // Create a new boss battle for the current user's boss level
    public Task<BossBattle> createBossBattle(int bossLevel) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Tasks.forException(new RuntimeException("User not authenticated"));
        }

        BossBattle bossBattle = new BossBattle(userId, bossLevel);
        bossBattle.setId("boss_" + userId); // Single active boss battle per user

        // For now, use hardcoded success rate of 67%
        bossBattle.setSuccessRate(0.67);

        // Save to Firebase
        return bossBattleRepository.createWithId(bossBattle)
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    return bossBattle;
                } else {
                    throw task.getException() != null ? task.getException() : new RuntimeException("Failed to create boss battle");
                }
            });
    }

    // Get current active boss battle for user
    public Task<BossBattle> getCurrentBossBattle() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Tasks.forException(new RuntimeException("User not authenticated"));
        }

        return userRepository.read(userId)
            .continueWithTask(task -> {
                User user = task.getResult();
                if (user == null) {
                    throw new RuntimeException("User not found");
                }

                int bossLevel = user.bossLevel != null ? user.bossLevel : 1;
                String bossBattleId = "boss_" + userId;

                // Try to load existing boss battle from Firebase
                return bossBattleRepository.read(bossBattleId)
                    .continueWithTask(readTask -> {
                        BossBattle existingBattle = readTask.getResult();

                        // If boss battle exists and is not finished, return it
                        if (existingBattle != null && !existingBattle.isDefeated()) {
                            return Tasks.forResult(existingBattle);
                        }

                        // Otherwise, create a new boss battle
                        return createBossBattle(bossLevel);
                    });
            });
    }

    // Perform an attack on the boss
    public Task<BattleResult> performAttack(BossBattle bossBattle) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Tasks.forException(new RuntimeException("User not authenticated"));
        }

        if (!userId.equals(bossBattle.getUserId())) {
            return Tasks.forException(new RuntimeException("Permission denied"));
        }

        return userRepository.read(userId)
            .continueWithTask(task -> {
                User user = task.getResult();
                if (user == null) {
                    throw new RuntimeException("User not found");
                }

                // Calculate total power points (base PP + equipment bonuses)
                return equipmentService.getActiveEquipment(userId)
                    .continueWith(equipmentTask -> {
                        int totalPowerPoints = user.powerPoints != null ? user.powerPoints : 0;

                        // Add equipment bonuses (simplified for now)
                        // In a full implementation, you'd calculate actual equipment bonuses

                        boolean attackHit = bossBattle.attack(totalPowerPoints);

                        BattleResult result = new BattleResult();
                        result.attackHit = attackHit;
                        result.damageDealt = attackHit ? totalPowerPoints : 0;
                        result.bossCurrentHp = bossBattle.getCurrentHp();
                        result.bossMaxHp = bossBattle.getMaxHp();
                        result.attacksRemaining = bossBattle.getAttacksRemaining();
                        result.battleFinished = bossBattle.isBattleFinished();
                        result.bossDefeated = bossBattle.isDefeated();

                        if (bossBattle.isDefeated()) {
                            result.coinsReward = bossBattle.getCoinsReward();
                            result.equipmentDropped = rollForEquipmentDrop(bossBattle);
                        }
                        else if (bossBattle.isBattleFinished()) {
                            if (bossBattle.getHpPercentage() < 0.5) {
                                bossBattle.reduceEquipmentDropChance();
                                bossBattle.reduceCoinsReward();
                            }

                            bossBattle.restartBattle();
                        }

                        // Save updated boss battle state to Firebase
                        bossBattleRepository.update(bossBattle);

                        return result;
                    });
            });
    }

    // Award battle rewards to the user
    public Task<Boolean> awardBattleRewards(BattleResult battleResult) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Tasks.forException(new RuntimeException("User not authenticated"));
        }

        return userRepository.read(userId)
            .continueWithTask(task -> {
                User user = task.getResult();
                if (user == null) {
                    throw new RuntimeException("User not found");
                }

                // Award coins
                user.coins = (user.coins != null ? user.coins : 0) + battleResult.coinsReward;

                // If boss was defeated, increment boss level for next battle
                if (battleResult.bossDefeated) {
                    user.bossLevel = (user.bossLevel != null ? user.bossLevel : 1) + 1;
                }

                Task<Void> updateUserTask = userRepository.update(user);

                // Create equipment if dropped
                if (battleResult.equipmentDropped != null) {
                    // In a full implementation, you'd save the equipment to the repository
                    // For now, we'll just return the update task
                    return updateUserTask.continueWith(updateTask -> true);
                } else {
                    return updateUserTask.continueWith(updateTask -> true);
                }
            });
    }

    // Roll for equipment drop based on boss battle outcome
    private Equipment rollForEquipmentDrop(BossBattle bossBattle) {
        double dropChance = bossBattle.getEquipmentDropChance();
        if (random.nextDouble() > dropChance) {
            return null; // No equipment dropped
        }

        String userId = getCurrentUserId();

        // 95% chance for clothing, 5% chance for weapon
        boolean isClothing = random.nextDouble() < 0.95;

        if (isClothing) {
            // Create random clothing
            Clothing.ClothingType[] clothingTypes = Clothing.ClothingType.values();
            Clothing.ClothingType randomType = clothingTypes[random.nextInt(clothingTypes.length)];
            return new Clothing(userId, randomType);
        } else {
            // Create random weapon
            Weapon.WeaponType[] weaponTypes = Weapon.WeaponType.values();
            Weapon.WeaponType randomType = weaponTypes[random.nextInt(weaponTypes.length)];
            return new Weapon(userId, randomType);
        }
    }

    // Battle result class to encapsulate attack results
    public static class BattleResult {
        public boolean attackHit;
        public int damageDealt;
        public int bossCurrentHp;
        public int bossMaxHp;
        public int attacksRemaining;
        public boolean battleFinished;
        public boolean bossDefeated;
        public int coinsReward;
        public Equipment equipmentDropped;

        public double getBossHpPercentage() {
            if (bossMaxHp == 0) return 0.0;
            return (double) bossCurrentHp / bossMaxHp;
        }
    }
}
