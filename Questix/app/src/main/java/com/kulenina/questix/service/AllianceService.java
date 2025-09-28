package com.kulenina.questix.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.kulenina.questix.model.Alliance;
import com.kulenina.questix.model.AllianceMessage;
import com.kulenina.questix.model.User;
import com.kulenina.questix.repository.AllianceRepository;
import com.kulenina.questix.repository.AllianceMessageRepository;
import com.kulenina.questix.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AllianceService {
    private final FirebaseFirestore db;
    private final AllianceRepository allianceRepository;
    private final AllianceMessageRepository messageRepository;
    private final UserRepository userRepository;

    public AllianceService() {
        this.db = FirebaseFirestore.getInstance();
        this.allianceRepository = new AllianceRepository();
        this.messageRepository = new AllianceMessageRepository();
        this.userRepository = new UserRepository();
    }

    public Task<String> createAlliance(String name, String leaderId) {
        return userRepository.read(leaderId)
            .continueWithTask(task -> {
                User leader = task.getResult();
                if (leader == null) {
                    throw new RuntimeException("User not found");
                }

                if (leader.isInAlliance()) {
                    throw new RuntimeException("User is already in an alliance");
                }

                // Create alliance
                String allianceId = UUID.randomUUID().toString();
                Alliance alliance = new Alliance(allianceId, name, leaderId);

                // Update user's current alliance
                leader.setCurrentAlliance(allianceId);

                // Batch operations
                WriteBatch batch = db.batch();
                batch.set(allianceRepository.getDocumentReference(allianceId), alliance);
                batch.update(userRepository.getDocumentReference(leaderId), "currentAllianceId", allianceId);

                return batch.commit().continueWith(commitTask -> allianceId);
            });
    }

    public Task<Void> inviteToAlliance(String allianceId, String inviterId, String inviteeId) {
        return Tasks.whenAllSuccess(
            allianceRepository.read(allianceId),
            userRepository.read(inviterId),
            userRepository.read(inviteeId)
        ).continueWithTask(task -> {
            List<Object> results = task.getResult();
            Alliance alliance = (Alliance) results.get(0);
            User inviter = (User) results.get(1);
            User invitee = (User) results.get(2);

            if (alliance == null) {
                throw new RuntimeException("Alliance not found");
            }
            if (inviter == null || invitee == null) {
                throw new RuntimeException("User not found");
            }

            // Check if inviter is leader or member
            if (!alliance.isMember(inviterId)) {
                throw new RuntimeException("Only alliance members can send invitations");
            }

            // Allow inviting users who are already in alliances
            // They can switch alliances by accepting the invitation

            // Check if invitee is already invited
            if (invitee.allianceInvitations != null && invitee.allianceInvitations.contains(allianceId)) {
                throw new RuntimeException("User already has a pending invitation to this alliance");
            }

            // Create invitation
            String invitationId = UUID.randomUUID().toString();
            // Note: We'll handle invitation creation in AllianceInvitationService
            // For now, just add to user's pending invitations
            invitee.addAllianceInvitation(invitationId);

            // Update invitee's pending invitations
            WriteBatch batch = db.batch();
            batch.update(userRepository.getDocumentReference(inviteeId),
                       "allianceInvitations", invitee.allianceInvitations);

            return batch.commit();
        });
    }

    public Task<Void> acceptInvitation(String invitationId, String userId) {
        // This will be handled by AllianceInvitationService
        // For now, just remove from user's pending invitations
        return userRepository.read(userId)
            .continueWithTask(task -> {
                User user = task.getResult();
                if (user == null) {
                    throw new RuntimeException("User not found");
                }

                user.removeAllianceInvitation(invitationId);

                WriteBatch batch = db.batch();
                batch.update(userRepository.getDocumentReference(userId),
                           "allianceInvitations", user.allianceInvitations);

                return batch.commit();
            });
    }

    public Task<Void> leaveAlliance(String allianceId, String userId) {
        return Tasks.whenAllSuccess(
            allianceRepository.read(allianceId),
            userRepository.read(userId)
        ).continueWithTask(task -> {
            List<Object> results = task.getResult();
            Alliance alliance = (Alliance) results.get(0);
            User user = (User) results.get(1);

            if (alliance == null) {
                throw new RuntimeException("Alliance not found");
            }
            if (user == null) {
                throw new RuntimeException("User not found");
            }

            if (!alliance.canLeave(userId)) {
                throw new RuntimeException("Cannot leave alliance: " +
                    (alliance.isLeader(userId) ? "Leader cannot leave" : "Cannot leave during active mission"));
            }

            // Remove user from alliance
            alliance.removeMember(userId);
            user.leaveAlliance();

            WriteBatch batch = db.batch();
            batch.update(allianceRepository.getDocumentReference(allianceId),
                       "memberIds", alliance.memberIds);
            batch.update(userRepository.getDocumentReference(userId),
                       "currentAllianceId", null);

            return batch.commit();
        });
    }

    public Task<Void> disbandAlliance(String allianceId, String leaderId) {
        return Tasks.whenAllSuccess(
            allianceRepository.read(allianceId),
            userRepository.read(leaderId)
        ).continueWithTask(task -> {
            List<Object> results = task.getResult();
            Alliance alliance = (Alliance) results.get(0);
            User leader = (User) results.get(1);

            if (alliance == null) {
                throw new RuntimeException("Alliance not found");
            }
            if (leader == null) {
                throw new RuntimeException("User not found");
            }

            if (!alliance.canDisband(leaderId)) {
                throw new RuntimeException("Cannot disband alliance: " +
                    (!alliance.isLeader(leaderId) ? "Only leader can disband" : "Cannot disband during active mission"));
            }

            // Remove alliance from all members
            List<Task<Void>> memberTasks = new ArrayList<>();
            for (String memberId : alliance.memberIds) {
                memberTasks.add(
                    userRepository.read(memberId)
                        .continueWithTask(memberTask -> {
                            User member = memberTask.getResult();
                            if (member != null) {
                                member.leaveAlliance();
                                return userRepository.update(member);
                            }
                            return Tasks.forResult(null);
                        })
                );
            }

            // Delete alliance and update all members
            return Tasks.whenAllSuccess(memberTasks)
                .continueWithTask(updateTask -> allianceRepository.delete(alliance));
        });
    }

    public Task<Alliance> getAlliance(String allianceId) {
        return allianceRepository.read(allianceId);
    }

    public Task<Alliance> getUserAlliance(String userId) {
        return userRepository.read(userId)
            .continueWithTask(task -> {
                User user = task.getResult();
                if (user == null || !user.isInAlliance()) {
                    return Tasks.forResult(null);
                }
                return allianceRepository.read(user.currentAllianceId);
            });
    }

    public Task<List<User>> getAllianceMembers(String allianceId) {
        return allianceRepository.read(allianceId)
            .continueWithTask(task -> {
                Alliance alliance = task.getResult();
                if (alliance == null || alliance.memberIds.isEmpty()) {
                    return Tasks.forResult(new ArrayList<>());
                }

                List<Task<User>> memberTasks = new ArrayList<>();
                for (String memberId : alliance.memberIds) {
                    memberTasks.add(userRepository.read(memberId));
                }

                return Tasks.whenAllSuccess(memberTasks);
            });
    }

    public Task<Void> setMissionActive(String allianceId, boolean active) {
        return allianceRepository.read(allianceId)
            .continueWithTask(task -> {
                Alliance alliance = task.getResult();
                if (alliance == null) {
                    throw new RuntimeException("Alliance not found");
                }

                alliance.setMissionActive(active);
                return allianceRepository.update(alliance);
            });
    }

    public Task<String> sendMessage(String allianceId, String senderId, String messageText) {
        return Tasks.whenAllSuccess(
            allianceRepository.read(allianceId),
            userRepository.read(senderId)
        ).continueWithTask(task -> {
            List<Object> results = task.getResult();
            Alliance alliance = (Alliance) results.get(0);
            User sender = (User) results.get(1);

            if (alliance == null) {
                throw new RuntimeException("Alliance not found");
            }
            if (sender == null) {
                throw new RuntimeException("User not found");
            }
            if (!alliance.isMember(senderId)) {
                throw new RuntimeException("User is not a member of this alliance");
            }

            // Create message
            String messageId = UUID.randomUUID().toString();
            AllianceMessage message = new AllianceMessage(messageId, allianceId, senderId,
                sender.username, messageText);

            return messageRepository.createWithId(message)
                .continueWith(createTask -> messageId);
        });
    }

    public Task<List<AllianceMessage>> getAllianceMessages(String allianceId) {
        return allianceRepository.read(allianceId)
            .continueWithTask(task -> {
                Alliance alliance = task.getResult();
                if (alliance == null) {
                    throw new RuntimeException("Alliance not found");
                }

                return messageRepository.getMessagesByAllianceId(allianceId);
            });
    }
}
