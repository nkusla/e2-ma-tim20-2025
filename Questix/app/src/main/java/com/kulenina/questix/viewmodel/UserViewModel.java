package com.kulenina.questix.viewmodel;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.lifecycle.ViewModel;

import com.kulenina.questix.model.User;
import com.kulenina.questix.service.LevelProgressionService;

public class UserViewModel extends BaseObservable {
    private User user;
    private boolean isLoading;
    private String errorMessage;
    private boolean isOwnProfile;
    private LevelProgressionService levelProgressionService;

    public UserViewModel() {
        this.isLoading = false;
        this.errorMessage = null;
        this.isOwnProfile = false;
        this.levelProgressionService = new LevelProgressionService();
    }

    @Bindable
    public boolean getIsLoading() {
        return isLoading;
    }

    public void setIsLoading(boolean isLoading) {
        this.isLoading = isLoading;
        notifyPropertyChanged(com.kulenina.questix.BR.isLoading);
    }

    @Bindable
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        notifyPropertyChanged(com.kulenina.questix.BR.errorMessage);
    }

    // User data getters
    @Bindable
    public String getEmail() {
        return user != null && user.email != null ? user.email : "Not set";
    }

    @Bindable
    public String getUsername() {
        return user != null && user.username != null ? user.username : "Not set";
    }

    @Bindable
    public String getLevel() {
        return user != null && user.level != null ? user.level.toString() : "1";
    }

    @Bindable
    public String getXp() {
        return user != null && user.xp != null ? user.xp.toString() : "0";
    }

    @Bindable
    public String getCoins() {
        return user != null && user.coins != null ? user.coins.toString() : "0";
    }

    @Bindable
    public String getPowerPoints() {
        return user != null && user.powerPoints != null ? user.powerPoints.toString() : "0";
    }

    @Bindable
    public String getAvatar() {
        return user != null && user.avatar != null ? user.avatar : "avatar_1";
    }

    @Bindable
    public String getTitle() {
        if (user != null && user.level != null) {
            return getTitleForLevel(user.level);
        }
        return getTitleForLevel(1);
    }

    private String getTitleForLevel(int level) {
        switch (level) {
            case 0: return "Beginner";
            case 1: return "Novice";
            case 2: return "Apprentice";
            case 3: return "Explorer";
            case 4: return "Adventurer";
            case 5: return "Seeker";
            case 6: return "Hunter";
            case 7: return "Scout";
            case 8: return "Guardian";
            case 9: return "Warrior";
            case 10: return "Knight";
            default: return "Beyond Legend";
        }
    }

    @Bindable
    public String getUserId() {
        return user != null && user.id != null ? user.id : "";
    }

    @Bindable
    public String getTotalXpForNextLevel() {
        if (user == null || user.level == null) return "200";

        int currentLevel = user.level;
        // Get total cumulative XP needed to reach next level
        return String.valueOf(levelProgressionService.getXpRequiredForLevel(currentLevel + 1));
    }

    @Bindable
    public int getXpProgressPercentage() {
        if (user == null || user.level == null || user.xp == null) return 0;

        int currentXp = user.xp;
        int totalXpForNextLevel = levelProgressionService.getXpRequiredForLevel(user.level + 1);

        if (totalXpForNextLevel <= 0) return 100;

        return Math.min(100, (currentXp * 100) / totalXpForNextLevel);
    }

    public void setUser(User user) {
        this.user = user;
        // Notify all user-related properties changed
        notifyPropertyChanged(com.kulenina.questix.BR.email);
        notifyPropertyChanged(com.kulenina.questix.BR.username);
        notifyPropertyChanged(com.kulenina.questix.BR.level);
        notifyPropertyChanged(com.kulenina.questix.BR.xp);
        notifyPropertyChanged(com.kulenina.questix.BR.coins);
        notifyPropertyChanged(com.kulenina.questix.BR.powerPoints);
        notifyPropertyChanged(com.kulenina.questix.BR.avatar);
        notifyPropertyChanged(com.kulenina.questix.BR.title);
        notifyPropertyChanged(com.kulenina.questix.BR.userId);
        notifyPropertyChanged(com.kulenina.questix.BR.totalXpForNextLevel);
        notifyPropertyChanged(com.kulenina.questix.BR.xpProgressPercentage);
    }

    public User getUser() {
        return user;
    }

    @Bindable
    public boolean getIsOwnProfile() {
        return isOwnProfile;
    }

    public void setIsOwnProfile(boolean isOwnProfile) {
        this.isOwnProfile = isOwnProfile;
        notifyPropertyChanged(com.kulenina.questix.BR.isOwnProfile);
    }
}
