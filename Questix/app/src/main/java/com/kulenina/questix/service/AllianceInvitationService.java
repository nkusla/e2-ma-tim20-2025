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

    public AllianceInvitationService() {
        this.db = FirebaseFirestore.getInstance();
        this.invitationRepository = new AllianceInvitationRepository();
        this.allianceRepository = new AllianceRepository();
        this.userRepository = new UserRepository();
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

            // Check if invitee is already in an alliance
            if (invitee.isInAlliance()) {
                throw new RuntimeException("User is already in an alliance");
            }

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

            return batch.commit().continueWith(commitTask -> invitationId);
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

            // Check if user is already in an alliance
            if (user.isInAlliance()) {
                throw new RuntimeException("User is already in an alliance");
            }

            // Get alliance and check if it still exists
            return allianceRepository.read(invitation.allianceId)
                .continueWithTask(allianceTask -> {
                    Alliance alliance = allianceTask.getResult();
                    if (alliance == null) {
                        throw new RuntimeException("Alliance no longer exists");
                    }

                    // Accept invitation
                    invitation.accept();
                    user.setCurrentAlliance(alliance.id);
                    user.removeAllianceInvitation(invitationId);
                    alliance.addMember(userId);

                    // Batch operations
                    WriteBatch batch = db.batch();
                    batch.update(invitationRepository.getDocumentReference(invitationId),
                               "status", invitation.status);
                    batch.update(userRepository.getDocumentReference(userId),
                               "currentAllianceId", alliance.id);
                    batch.update(userRepository.getDocumentReference(userId),
                               "allianceInvitations", user.allianceInvitations);
                    batch.update(allianceRepository.getDocumentReference(alliance.id),
                               "memberIds", alliance.memberIds);

                    return batch.commit();
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
