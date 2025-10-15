package com.kulenina.questix;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.kulenina.questix.service.NotificationSenderService;

public class QuestixApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        NotificationSenderService notificationService = new NotificationSenderService();
        notificationService.initializeFcmToken();
    }
}
