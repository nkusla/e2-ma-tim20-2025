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
    public int coinsReward;
    public double equipmentDropChance = 0.2;
    public double successRate;

    public BossBattle() {
        this.attacksRemaining = 5;
        this.isDefeated = false;
        this.successRate = 0.67;
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

    public static int calculateBossHp(int bossLevel) {
        if (bossLevel <= 1) return 200;

        int previousHp = calculateBossHp(bossLevel - 1);
        return Math.round(previousHp * 2 + previousHp / 2.0f);
    }

    public static int calculateCoinsReward(int bossLevel) {
        if (bossLevel <= 1) return 200;

        int previousReward = calculateCoinsReward(bossLevel - 1);
        return Math.round(previousReward * 1.2f);
    }

    public boolean attack(int userPowerPoints) {
        if (attacksRemaining <= 0 || isDefeated) {
            return false;
        }

        attacksRemaining--;

        double random = Math.random();
        boolean hits = random < successRate;

        if (hits) {
            setCurrentHp(currentHp - userPowerPoints);

            if (currentHp <= 0) {
                isDefeated = true;
            }
        }

        return hits;
    }

    public boolean isBattleFinished() {
        return attacksRemaining <= 0;
    }

    public double getHpPercentage() {
        if (maxHp == 0) return 0.0;
        return (double) currentHp / maxHp;
    }

    public double getEquipmentDropChance() {
        return equipmentDropChance;
    }

    public void reduceEquipmentDropChance() {
        equipmentDropChance /= 2;
    }

    public void reduceCoinsReward() {
        coinsReward = Math.round(coinsReward / 2.0f);
    }

    public void restartBattle() {
        this.attacksRemaining = 5;
        this.currentHp = this.maxHp;
        this.isDefeated = false;
    }
}
