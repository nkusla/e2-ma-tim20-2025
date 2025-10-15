package com.kulenina.questix.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.kulenina.questix.R;
import com.kulenina.questix.activity.MainActivity;

public class NotificationFirebaseMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "questix_notifications";
    private static final String CHANNEL_NAME = "Questix Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for alliance invitations and updates";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            handleDataMessage(remoteMessage);
        }
        if (remoteMessage.getNotification() != null) {
            showNotification(
                remoteMessage.getNotification().getTitle(),
                remoteMessage.getNotification().getBody(),
                remoteMessage.getData()
            );
        }
    }

    @Override
    public void onNewToken(String token) {
        NotificationSenderService notificationService = new NotificationSenderService();
        notificationService.updateUserFcmToken(token);
    }

    private void handleDataMessage(RemoteMessage remoteMessage) {
        String type = remoteMessage.getData().get("type");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");

        if (type != null) {
            switch (type) {
                case "alliance_invitation":
                    showNotification(title != null ? title : "Alliance Invitation",
                                   body != null ? body : "You have received an alliance invitation",
                                   remoteMessage.getData());
                    break;
                case "alliance_invitation_accepted":
                    showNotification(title != null ? title : "Invitation Accepted",
                                   body != null ? body : "Someone accepted your alliance invitation",
                                   remoteMessage.getData());
                    break;
                default:
                    showNotification(title, body, remoteMessage.getData());
                    break;
            }
        }
    }

    private void showNotification(String title, String body, java.util.Map<String, String> data) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (data != null) {
            for (java.util.Map.Entry<String, String> entry : data.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
