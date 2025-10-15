package com.kulenina.questix.service;


import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.kulenina.questix.model.User;
import com.kulenina.questix.repository.UserRepository;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationSenderService {
    private static final String NOTIFICATION_SERVER_URL = "http://localhost:3000";  // localhost server URL for testing

    private final FirebaseFirestore db;
    private final UserRepository userRepository;
    private final OkHttpClient httpClient;

    public NotificationSenderService() {
        this.db = FirebaseFirestore.getInstance();
        this.userRepository = new UserRepository();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void initializeFcmToken() {
    FirebaseMessaging.getInstance().getToken()
        .addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                return;
            }

            String token = task.getResult();
            updateUserFcmToken(token);
        });
    }

    public void updateUserFcmToken(String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            userRepository.read(userId)
                    .addOnSuccessListener(user -> {
                        if (user != null) {
                            user.setFcmToken(token);
                            userRepository.update(user);
                        }
                    })
;
        }
    }

    public Task<Void> sendAllianceInvitationNotification(String inviteeId, String allianceName, String inviterUsername) {
        return userRepository.read(inviteeId)
                .continueWithTask(task -> {
                    User invitee = task.getResult();
                    if (invitee == null) {
                        return Tasks.forResult(null);
                    }

                    if (!invitee.hasFcmToken()) {
                        return Tasks.forResult(null);
                    }

                    String title = "Alliance Invitation";
                    String body = inviterUsername + " invited you to join \"" + allianceName + "\"";

                    Map<String, String> data = new HashMap<>();
                    data.put("type", "alliance_invitation");
                    data.put("alliance_name", allianceName);
                    data.put("inviter_username", inviterUsername);

                    return sendNotification(invitee.getFcmToken(), title, body, data);
                });
    }

    public Task<Void> sendInvitationAcceptedNotification(String leaderId, String accepterUsername, String allianceName) {
        return userRepository.read(leaderId)
                .continueWithTask(task -> {
                    User leader = task.getResult();
                    if (leader == null || !leader.hasFcmToken()) {
                        return Tasks.forResult(null);
                    }

                    String title = "Invitation Accepted";
                    String body = accepterUsername + " joined your alliance \"" + allianceName + "\"";

                    Map<String, String> data = new HashMap<>();
                    data.put("type", "alliance_invitation_accepted");
                    data.put("alliance_name", allianceName);
                    data.put("accepter_username", accepterUsername);

                    return sendNotification(leader.getFcmToken(), title, body, data);
                });
    }

    private Task<Void> sendNotification(String fcmToken, String title, String body, Map<String, String> data) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return Tasks.forException(new RuntimeException("User not authenticated"));
        }

        return currentUser.getIdToken(false).continueWithTask(tokenTask -> {
            if (!tokenTask.isSuccessful()) {
                return Tasks.forException(new RuntimeException("Failed to get ID token", tokenTask.getException()));
            }

            String idToken = tokenTask.getResult().getToken();
            TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();

            try {
                JSONObject json = new JSONObject();
                json.put("fcmToken", fcmToken);
                json.put("title", title);
                json.put("body", body);

                if (data != null) {
                    JSONObject dataJson = new JSONObject();
                    for (Map.Entry<String, String> entry : data.entrySet()) {
                        dataJson.put(entry.getKey(), entry.getValue());
                    }
                    json.put("data", dataJson);
                }

                RequestBody requestBody = RequestBody.create(
                        json.toString(),
                        MediaType.get("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(NOTIFICATION_SERVER_URL + "/send-notification")
                        .post(requestBody)
                        .addHeader("Authorization", "Bearer " + idToken)
                        .addHeader("Content-Type", "application/json")
                        .build();

                httpClient.newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(okhttp3.Call call, IOException e) {
                        taskCompletionSource.setException(e);
                    }

                    @Override
                    public void onResponse(okhttp3.Call call, Response response) throws IOException {
                        try (response) {
                            if (!response.isSuccessful()) {
                                String responseBody = response.body() != null ? response.body().string() : "No response body";
                                taskCompletionSource.setException(new IOException("HTTP " + response.code() + ": " + response.message() + " - " + responseBody));
                            } else {
                                taskCompletionSource.setResult(null);
                            }
                        } catch (IOException e) {
                            taskCompletionSource.setException(e);
                        }
                    }
                });

                return taskCompletionSource.getTask();
            } catch (JSONException e) {
                return Tasks.forException(new RuntimeException(e));
            }
        });
    }

}
