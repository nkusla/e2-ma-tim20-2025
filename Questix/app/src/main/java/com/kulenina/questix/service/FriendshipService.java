package com.kulenina.questix.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.kulenina.questix.model.User;
import com.kulenina.questix.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class FriendshipService {
    private final FirebaseFirestore db;
    private final UserRepository userRepository;

    public FriendshipService() {
        this.db = FirebaseFirestore.getInstance();
        this.userRepository = new UserRepository();
    }

    public Task<Void> addFriend(String currentUserId, String friendId) {
        return Tasks.whenAllSuccess(
            userRepository.read(currentUserId),
            userRepository.read(friendId)
        ).continueWithTask(task -> {
            List<Object> results = task.getResult();
            User currentUser = (User) results.get(0);
            User friendUser = (User) results.get(1);

            if (currentUser == null || friendUser == null) {
                throw new RuntimeException("User not found");
            }

            if (currentUser.isFriend(friendId)) {
                throw new RuntimeException("Already friends");
            }

            // Add friend to both users
            currentUser.addFriend(friendId);
            friendUser.addFriend(currentUserId);

            // Batch update both users
            WriteBatch batch = db.batch();
            batch.update(userRepository.getDocumentReference(currentUserId), "friends", currentUser.friends);
            batch.update(userRepository.getDocumentReference(friendId), "friends", friendUser.friends);

            return batch.commit();
        });
    }


    public Task<Void> removeFriend(String currentUserId, String friendId) {
        return Tasks.whenAllSuccess(
            userRepository.read(currentUserId),
            userRepository.read(friendId)
        ).continueWithTask(task -> {
            List<Object> results = task.getResult();
            User currentUser = (User) results.get(0);
            User friendUser = (User) results.get(1);

            if (currentUser == null || friendUser == null) {
                throw new RuntimeException("User not found");
            }

            currentUser.removeFriend(friendId);
            friendUser.removeFriend(currentUserId);

            WriteBatch batch = db.batch();
            batch.update(userRepository.getDocumentReference(currentUserId), "friends", currentUser.friends);
            batch.update(userRepository.getDocumentReference(friendId), "friends", friendUser.friends);

            return batch.commit();
        });
    }


    public Task<List<User>> getFriends(String userId) {
        return userRepository.read(userId)
            .continueWithTask(task -> {
                User user = task.getResult();
                if (user == null || user.friends.isEmpty()) {
                    return Tasks.forResult(new ArrayList<>());
                }

                List<Task<User>> friendTasks = new ArrayList<>();
                for (String friendId : user.friends) {
                    friendTasks.add(userRepository.read(friendId));
                }

                return Tasks.whenAllSuccess(friendTasks);
            });
    }

    public Task<Boolean> areFriends(String userId1, String userId2) {
        return userRepository.read(userId1)
            .continueWith(task -> {
                User user = task.getResult();
                return user != null && user.isFriend(userId2);
            });
    }

    public Task<List<User>> getAllUsers() {
        return userRepository.readAll();
    }
}
