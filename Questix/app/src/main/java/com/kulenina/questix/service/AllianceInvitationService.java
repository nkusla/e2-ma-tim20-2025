package com.kulenina.questix.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.kulenina.questix.model.Alliance;
import com.kulenina.questix.model.AllianceInvitation;
import com.kulenina.questix.model.User;
import com.kulenina.questix.repository.AllianceInvitationRepository;
import com.kulenina.questix.repository.AllianceRepository;
import com.kulenina.questix.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AllianceInvitationService {
    private final FirebaseFirestore db;
    private final AllianceInvitationRepository invitationRepository;
    private final AllianceRepository allianceRepository;
    private final UserRepository userRepository;
    private final NotificationSenderService notificationService;

    public AllianceInvitationService() {
        this.db = FirebaseFirestore.getInstance();
        this.invitationRepository = new AllianceInvitationRepository();
        this.allianceRepository = new AllianceRepository();
        this.userRepository = new UserRepository();
        this.notificationService = new NotificationSenderService();
    }

    public Task<String> createInvitation(String allianceId, String inviterId, String inviteeId) {
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

            // Check if inviter is member of alliance
            if (!alliance.isMember(inviterId)) {
                throw new RuntimeException("Only alliance members can send invitations");
            }

            // Allow users to receive invitations even if they're in an alliance
            // They can switch alliances by accepting the invitation

            // Check if there's already a pending invitation
            if (hasPendingInvitation(allianceId, inviteeId)) {
                throw new RuntimeException("User already has a pending invitation to this alliance");
            }

            // Create invitation
            String invitationId = UUID.randomUUID().toString();
            AllianceInvitation invitation = new AllianceInvitation(
                invitationId, allianceId, alliance.name,
                inviterId, inviter.username, inviteeId
            );

            // Add to invitee's pending invitations
            invitee.addAllianceInvitation(invitationId);

            // Batch operations
            WriteBatch batch = db.batch();
            batch.set(invitationRepository.getDocumentReference(invitationId), invitation);
            batch.update(userRepository.getDocumentReference(inviteeId),
                       "allianceInvitations", invitee.allianceInvitations);

            return batch.commit().continueWith(commitTask -> {
                // Send notification to invitee
                notificationService.sendAllianceInvitationNotification(
                    inviteeId, alliance.name, inviter.username)
                    .addOnSuccessListener(aVoid ->
                        android.util.Log.d("AllianceInvitationService", "Notification sent successfully for invitation: " + invitationId))
                    .addOnFailureListener(e ->
                        android.util.Log.e("AllianceInvitationService", "Failed to send notification for invitation: " + invitationId, e));
                return invitationId;
            });
        });
    }

    public Task<Void> acceptInvitation(String invitationId, String userId) {
        return Tasks.whenAllSuccess(
            invitationRepository.read(invitationId),
            userRepository.read(userId)
        ).continueWithTask(task -> {
            List<Object> results = task.getResult();
            AllianceInvitation invitation = (AllianceInvitation) results.get(0);
            User user = (User) results.get(1);

            if (invitation == null) {
                throw new RuntimeException("Invitation not found");
            }
            if (user == null) {
                throw new RuntimeException("User not found");
            }

            if (!invitation.canBeRespondedTo()) {
                throw new RuntimeException("Invitation has already been responded to");
            }

            if (!invitation.inviteeId.equals(userId)) {
                throw new RuntimeException("This invitation is not for this user");
            }

            // Get the new alliance and check if it still exists
            return allianceRepository.read(invitation.allianceId)
                .continueWithTask(allianceTask -> {
                    Alliance newAlliance = allianceTask.getResult();
                    if (newAlliance == null) {
                        throw new RuntimeException("Alliance no longer exists");
                    }

                    // If user is already in an alliance, we need to handle leaving the old one
                    Task<Alliance> oldAllianceTask = null;
                    if (user.isInAlliance()) {
                        oldAllianceTask = allianceRepository.read(user.currentAllianceId);
                    } else {
                        oldAllianceTask = Tasks.forResult(null);
                    }

                    return oldAllianceTask.continueWithTask(oldAllianceTaskResult -> {
                        Alliance oldAlliance = oldAllianceTaskResult.getResult();

                        // Accept invitation
                        invitation.accept();
                        user.removeAllianceInvitation(invitationId);

                        // Remove from old alliance if exists
                        if (oldAlliance != null) {
                            oldAlliance.removeMember(userId);
                        }

                        // Add to new alliance
                        user.setCurrentAlliance(newAlliance.id);
                        newAlliance.addMember(userId);

                        // Batch operations
                        WriteBatch batch = db.batch();
                        batch.update(invitationRepository.getDocumentReference(invitationId),
                                   "status", invitation.status);
                        batch.update(userRepository.getDocumentReference(userId),
                                   "currentAllianceId", newAlliance.id);
                        batch.update(userRepository.getDocumentReference(userId),
                                   "allianceInvitations", user.allianceInvitations);
                        batch.update(allianceRepository.getDocumentReference(newAlliance.id),
                                   "memberIds", newAlliance.memberIds);

                        // Update old alliance if it existed
                        if (oldAlliance != null) {
                            batch.update(allianceRepository.getDocumentReference(oldAlliance.id),
                                       "memberIds", oldAlliance.memberIds);
                        }

                        return batch.commit().continueWithTask(batchTask -> {
                            // Send notification to alliance leader about acceptance
                            notificationService.sendInvitationAcceptedNotification(
                                newAlliance.leaderId, user.username, newAlliance.name);

                            return Tasks.forResult(null);
                        });
                    });
                });
        });
    }

    public Task<Void> declineInvitation(String invitationId, String userId) {
        return Tasks.whenAllSuccess(
            invitationRepository.read(invitationId),
            userRepository.read(userId)
        ).continueWithTask(task -> {
            List<Object> results = task.getResult();
            AllianceInvitation invitation = (AllianceInvitation) results.get(0);
            User user = (User) results.get(1);

            if (invitation == null) {
                throw new RuntimeException("Invitation not found");
            }
            if (user == null) {
                throw new RuntimeException("User not found");
            }

            if (!invitation.canBeRespondedTo()) {
                throw new RuntimeException("Invitation has already been responded to");
            }

            if (!invitation.inviteeId.equals(userId)) {
                throw new RuntimeException("This invitation is not for this user");
            }

            // Decline invitation
            invitation.decline();
            user.removeAllianceInvitation(invitationId);

            // Batch operations
            WriteBatch batch = db.batch();
            batch.update(invitationRepository.getDocumentReference(invitationId),
                       "status", invitation.status);
            batch.update(userRepository.getDocumentReference(userId),
                       "allianceInvitations", user.allianceInvitations);

            return batch.commit();
        });
    }

    public Task<List<AllianceInvitation>> getUserPendingInvitations(String userId) {
        return userRepository.read(userId)
            .continueWithTask(task -> {
                User user = task.getResult();
                if (user == null || user.allianceInvitations == null || user.allianceInvitations.isEmpty()) {
                    return Tasks.forResult(new ArrayList<>());
                }

                List<Task<AllianceInvitation>> invitationTasks = new ArrayList<>();
                for (String invitationId : user.allianceInvitations) {
                    invitationTasks.add(invitationRepository.read(invitationId));
                }

                return Tasks.whenAllSuccess(invitationTasks);
            });
    }

    public Task<List<AllianceInvitation>> getAllianceInvitations(String allianceId) {
        // This would require a query by allianceId field
        // For now, return empty list - can be implemented with Firestore queries
        return Tasks.forResult(new ArrayList<>());
    }

    public Task<AllianceInvitation> getInvitation(String invitationId) {
        return invitationRepository.read(invitationId);
    }

    public Task<Void> deleteInvitation(String invitationId) {
        return invitationRepository.delete(invitationId);
    }

    private boolean hasPendingInvitation(String allianceId, String userId) {
        // This would require a query to check for pending invitations
        // For now, return false - can be implemented with Firestore queries
        return false;
    }
}
