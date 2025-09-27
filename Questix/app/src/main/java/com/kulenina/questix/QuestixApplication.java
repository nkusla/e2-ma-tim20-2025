package com.kulenina.questix;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class QuestixApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
