package com.kulenina.questix.viewmodel;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

public class QuestsViewModel extends BaseObservable {
    private String questsTitle = "Available Quests";
    private String questsDescription = "Complete quests to earn rewards and level up!";

    @Bindable
    public String getQuestsTitle() {
        return questsTitle;
    }

    public void setQuestsTitle(String questsTitle) {
        this.questsTitle = questsTitle;
        notifyPropertyChanged(com.kulenina.questix.BR.questsTitle);
    }

    @Bindable
    public String getQuestsDescription() {
        return questsDescription;
    }

    public void setQuestsDescription(String questsDescription) {
        this.questsDescription = questsDescription;
        notifyPropertyChanged(com.kulenina.questix.BR.questsDescription);
    }
}
