package com.kulenina.questix.viewmodel;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.lifecycle.ViewModel;

import com.kulenina.questix.model.User;

public class UserViewModel extends BaseObservable {
    private User user;
    private boolean isLoading;
    private String errorMessage;

    public UserViewModel() {
        this.isLoading = false;
        this.errorMessage = null;
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

    public void setUser(User user) {
        this.user = user;
        // Notify all user-related properties changed
        notifyPropertyChanged(com.kulenina.questix.BR.email);
        notifyPropertyChanged(com.kulenina.questix.BR.username);
        notifyPropertyChanged(com.kulenina.questix.BR.level);
        notifyPropertyChanged(com.kulenina.questix.BR.xp);
        notifyPropertyChanged(com.kulenina.questix.BR.coins);
        notifyPropertyChanged(com.kulenina.questix.BR.powerPoints);
    }

    public User getUser() {
        return user;
    }
}
