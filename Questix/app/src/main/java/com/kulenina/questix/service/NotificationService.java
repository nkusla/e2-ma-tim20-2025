package com.kulenina.questix.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.kulenina.questix.R;
import com.kulenina.questix.activity.MainActivity;

public class NotificationService {
    private static final String CHANNEL_ID = "alliance_notifications";
    private static final String CHANNEL_NAME = "Alliance Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for alliance invitations and updates";

    private static final int INVITATION_NOTIFICATION_ID = 1001;
    private static final int ACCEPTANCE_NOTIFICATION_ID = 1002;

    private final Context context;
    private final NotificationManagerCompat notificationManager;

    public NotificationService(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        createNotificationChannel();
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

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public void showAllianceInvitationNotification(String allianceName, String inviterName) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("action", "alliance_invitation");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_group) // You'll need to add this icon
            .setContentTitle("Alliance Invitation")
            .setContentText(inviterName + " invited you to join " + allianceName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false) // Don't auto-cancel - user must respond
            .setContentIntent(pendingIntent)
            .setOngoing(true); // Makes notification persistent

        notificationManager.notify(INVITATION_NOTIFICATION_ID, builder.build());
    }

    public void showInvitationAcceptedNotification(String userName, String allianceName) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("action", "alliance_accepted");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_group) // You'll need to add this icon
            .setContentTitle("Alliance Update")
            .setContentText(userName + " joined " + allianceName)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        notificationManager.notify(ACCEPTANCE_NOTIFICATION_ID, builder.build());
    }

    public void cancelInvitationNotification() {
        notificationManager.cancel(INVITATION_NOTIFICATION_ID);
    }

    public void cancelAllNotifications() {
        notificationManager.cancelAll();
    }

    public void showAllianceMessageNotification(String allianceName, String senderName, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("action", "alliance_message");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_group)
            .setContentTitle(allianceName)
            .setContentText(senderName + ": " + message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
