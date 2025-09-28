package com.kulenina.questix.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.kulenina.questix.R;
import com.kulenina.questix.adapter.AllianceInvitationAdapter;
import com.kulenina.questix.databinding.FragmentAllianceInvitationsBinding;
import com.kulenina.questix.model.AllianceInvitation;
import com.kulenina.questix.service.AllianceInvitationService;

import java.util.List;

public class AllianceInvitationFragment extends Fragment implements AllianceInvitationAdapter.OnInvitationActionListener {
    private FragmentAllianceInvitationsBinding binding;
    private AllianceInvitationService invitationService;
    private String currentUserId;
    private List<AllianceInvitation> pendingInvitations;
    private AllianceInvitationAdapter invitationAdapter;

    public interface OnInvitationUpdateListener {
        void onInvitationAccepted();
        void onInvitationDeclined();
    }

    private OnInvitationUpdateListener updateListener;

    public void setOnInvitationUpdateListener(OnInvitationUpdateListener listener) {
        this.updateListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_alliance_invitations, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        invitationService = new AllianceInvitationService();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupUI();
        loadPendingInvitations();
    }

    private void setupUI() {
        // Setup invitation RecyclerView
        invitationAdapter = new AllianceInvitationAdapter(this);
        binding.recyclerViewInvitations.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewInvitations.setAdapter(invitationAdapter);
    }

    public void loadPendingInvitations() {
        invitationService.getUserPendingInvitations(currentUserId)
            .addOnSuccessListener(invitations -> {
                pendingInvitations = invitations;
                updateInvitationsUI();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error loading invitations: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                pendingInvitations = null;
                updateInvitationsUI();
            });
    }

    private void updateInvitationsUI() {
        if (pendingInvitations != null && !pendingInvitations.isEmpty()) {
            binding.recyclerViewInvitations.setVisibility(View.VISIBLE);
            binding.textViewNoInvitations.setVisibility(View.GONE);

            // Update the adapter with the invitation list
            invitationAdapter.updateInvitations(pendingInvitations);
        } else {
            binding.recyclerViewInvitations.setVisibility(View.GONE);
            binding.textViewNoInvitations.setVisibility(View.VISIBLE);
        }
    }

    public boolean hasInvitations() {
        return pendingInvitations != null && !pendingInvitations.isEmpty();
    }

    public int getInvitationCount() {
        return pendingInvitations != null ? pendingInvitations.size() : 0;
    }

    @Override
    public void onAcceptInvitation(AllianceInvitation invitation) {
        invitationService.acceptInvitation(invitation.id, currentUserId)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Invitation accepted! You joined " + invitation.allianceName, Toast.LENGTH_SHORT).show();
                // Remove the invitation from the list
                invitationAdapter.removeInvitation(invitation);
                // Reload invitations to update the count
                loadPendingInvitations();
                // Notify parent fragment
                if (updateListener != null) {
                    updateListener.onInvitationAccepted();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error accepting invitation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public void onDeclineInvitation(AllianceInvitation invitation) {
        invitationService.declineInvitation(invitation.id, currentUserId)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Invitation declined", Toast.LENGTH_SHORT).show();
                // Remove the invitation from the list
                invitationAdapter.removeInvitation(invitation);
                // Reload invitations to update the count
                loadPendingInvitations();
                // Notify parent fragment
                if (updateListener != null) {
                    updateListener.onInvitationDeclined();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error declining invitation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}
