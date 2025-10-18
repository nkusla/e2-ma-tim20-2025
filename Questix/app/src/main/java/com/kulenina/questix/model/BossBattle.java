package com.kulenina.questix.model;

import java.io.Serializable;

public class BossBattle implements IIdentifiable, Serializable {
    public String id;
    public String userId;
    public int bossLevel;
    public int maxHp;
    public int currentHp;
    public int attacksRemaining;
    public boolean isDefeated;
    public boolean isActive;
    public int coinsReward;
    public double successRate; // Percentage chance of hitting the boss (0.0 - 1.0)

    public BossBattle() {
        this.attacksRemaining = 5;
        this.isDefeated = false;
        this.isActive = false;
        this.successRate = 0.67; // Default 67% success rate
    }

    public BossBattle(String userId, int bossLevel) {
        this();
        this.userId = userId;
        this.bossLevel = bossLevel;
        this.maxHp = calculateBossHp(bossLevel);
        this.currentHp = this.maxHp;
        this.coinsReward = calculateCoinsReward(bossLevel);
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getBossLevel() {
        return bossLevel;
    }

    public void setBossLevel(int bossLevel) {
        this.bossLevel = bossLevel;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public void setCurrentHp(int currentHp) {
        this.currentHp = Math.max(0, currentHp);
        if (this.currentHp == 0) {
            this.isDefeated = true;
        }
    }

    public int getAttacksRemaining() {
        return attacksRemaining;
    }

    public void setAttacksRemaining(int attacksRemaining) {
        this.attacksRemaining = Math.max(0, attacksRemaining);
    }

    public boolean isDefeated() {
        return isDefeated;
    }

    public void setDefeated(boolean defeated) {
        this.isDefeated = defeated;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public int getCoinsReward() {
        return coinsReward;
    }

    public void setCoinsReward(int coinsReward) {
        this.coinsReward = coinsReward;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = Math.max(0.0, Math.min(1.0, successRate));
    }

    // Calculate boss HP based on level
    // First boss (level 1) has 200 HP
    // Each subsequent boss: HP = previous HP * 2 + previous HP / 2
    public static int calculateBossHp(int bossLevel) {
        if (bossLevel <= 1) return 200;

        int previousHp = calculateBossHp(bossLevel - 1);
        return Math.round(previousHp * 2 + previousHp / 2.0f);
    }

    // Calculate coins reward based on boss level
    // First boss gives 200 coins, each subsequent boss gives 20% more
    public static int calculateCoinsReward(int bossLevel) {
        if (bossLevel <= 1) return 200;

        int previousReward = calculateCoinsReward(bossLevel - 1);
        return Math.round(previousReward * 1.2f);
    }

    // Perform an attack on the boss
    public boolean attack(int userPowerPoints) {
        if (attacksRemaining <= 0 || isDefeated) {
            return false;
        }

        attacksRemaining--;

        // Generate random number to determine if attack hits
        double random = Math.random();
        boolean hits = random < successRate;

        if (hits) {
            setCurrentHp(currentHp - userPowerPoints);
        }

        return hits;
    }

    // Check if boss battle is finished (either defeated or no attacks remaining)
    public boolean isBattleFinished() {
        return isDefeated || attacksRemaining <= 0;
    }

    // Get HP percentage remaining
    public double getHpPercentage() {
        if (maxHp == 0) return 0.0;
        return (double) currentHp / maxHp;
    }

    // Check if boss took at least 50% damage (for partial rewards)
    public boolean hasHalfDamage() {
        return getHpPercentage() <= 0.5;
    }

    // Get final coins reward based on battle outcome
    public int getFinalCoinsReward() {
        if (isDefeated) {
            return coinsReward;
        } else if (hasHalfDamage()) {
            return coinsReward / 2;
        } else {
            return 0;
        }
    }

    // Get equipment drop chance based on battle outcome
    public double getEquipmentDropChance() {
        if (isDefeated) {
            return 0.2; // 20% chance
        } else if (hasHalfDamage()) {
            return 0.1; // 10% chance (half of normal)
        } else {
            return 0.0; // No chance
        }
    }
}
