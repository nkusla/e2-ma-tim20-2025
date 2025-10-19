package com.kulenina.questix.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class StatisticsData implements Serializable {
    public int activeDaysCount;
    public int totalCreatedTasks;
    public int totalCompletedTasks;
    public int totalMissedTasks;
    public int totalCanceledTasks;
    public int longestSuccessStreak;
    public Map<String, Integer> completedTasksByCategory;
    public List<DifficultyXpData> averageDifficultyData;
    public List<XpProgressData> last7DaysXp;
    public int startedSpecialMissions;
    public int completedSpecialMissions;

    public StatisticsData() {
    }

    public static class DifficultyXpData implements Serializable {
        public String difficulty;
        public double averageXp;
        public int count;

        public DifficultyXpData(String difficulty, double averageXp, int count) {
            this.difficulty = difficulty;
            this.averageXp = averageXp;
            this.count = count;
        }
    }

    public static class XpProgressData implements Serializable {
        public String date;
        public int xpEarned;

        public XpProgressData(String date, int xpEarned) {
            this.date = date;
            this.xpEarned = xpEarned;
        }
    }
}
