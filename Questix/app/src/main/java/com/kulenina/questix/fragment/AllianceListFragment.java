package com.kulenina.questix.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.kulenina.questix.R;
import com.kulenina.questix.databinding.FragmentAllianceListBinding;
import com.kulenina.questix.model.Alliance;
import com.kulenina.questix.model.AllianceInvitation;
import com.kulenina.questix.model.User;
import com.kulenina.questix.service.AllianceService;
import com.kulenina.questix.service.AllianceInvitationService;
import com.kulenina.questix.service.AuthService;
import com.kulenina.questix.dialog.CreateAllianceDialog;

import java.util.List;

public class AllianceListFragment extends Fragment implements CreateAllianceDialog.OnAllianceCreatedListener {
    private FragmentAllianceListBinding binding;
    private AllianceService allianceService;
    private AllianceInvitationService invitationService;
    private AuthService authService;
    private String currentUserId;
    private Alliance currentAlliance;
    private List<AllianceInvitation> pendingInvitations;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_alliance_list, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        allianceService = new AllianceService();
        invitationService = new AllianceInvitationService();
        authService = new AuthService();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupUI();
        loadUserAlliance();
        loadPendingInvitations();
    }

    private void setupUI() {
        binding.buttonCreateAlliance.setOnClickListener(v -> showCreateAllianceDialog());
        binding.buttonInviteFriends.setOnClickListener(v -> showInviteFriendsDialog());
        binding.buttonLeaveAlliance.setOnClickListener(v -> leaveAlliance());
        binding.buttonDisbandAlliance.setOnClickListener(v -> disbandAlliance());
    }

    private void loadUserAlliance() {
        allianceService.getUserAlliance(currentUserId)
            .addOnSuccessListener(alliance -> {
                currentAlliance = alliance;
                updateAllianceUI();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error loading alliance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                updateAllianceUI();
            });
    }

    private void loadPendingInvitations() {
        invitationService.getUserPendingInvitations(currentUserId)
            .addOnSuccessListener(invitations -> {
                pendingInvitations = invitations;
                updateInvitationsUI();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error loading invitations: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                updateInvitationsUI();
            });
    }

    private void updateAllianceUI() {
        if (currentAlliance != null) {
            binding.textViewAllianceName.setText(currentAlliance.name);
            binding.textViewAllianceDescription.setText(currentAlliance.description);
            binding.textViewMemberCount.setText("Members: " + currentAlliance.getMemberCount());
            binding.textViewMissionStatus.setText(currentAlliance.isMissionActive ? "Mission Active" : "No Active Mission");

            // Show/hide buttons based on user role and alliance status
            binding.buttonInviteFriends.setVisibility(View.VISIBLE);
            binding.buttonLeaveAlliance.setVisibility(currentAlliance.canLeave(currentUserId) ? View.VISIBLE : View.GONE);
            binding.buttonDisbandAlliance.setVisibility(currentAlliance.canDisband(currentUserId) ? View.VISIBLE : View.GONE);

            binding.layoutAllianceInfo.setVisibility(View.VISIBLE);
            binding.layoutNoAlliance.setVisibility(View.GONE);
        } else {
            binding.layoutAllianceInfo.setVisibility(View.GONE);
            binding.layoutNoAlliance.setVisibility(View.VISIBLE);
        }
    }

    private void updateInvitationsUI() {
        if (pendingInvitations != null && !pendingInvitations.isEmpty()) {
            binding.textViewInvitationCount.setText("Pending Invitations: " + pendingInvitations.size());
            binding.layoutInvitations.setVisibility(View.VISIBLE);

            // For now, just show the count. In a real implementation, you'd show a list
            binding.textViewInvitationList.setText("You have " + pendingInvitations.size() + " pending alliance invitations");
        } else {
            binding.layoutInvitations.setVisibility(View.GONE);
        }
    }

    private void showCreateAllianceDialog() {
        CreateAllianceDialog dialog = CreateAllianceDialog.newInstance(this);
        dialog.show(getParentFragmentManager(), "CreateAllianceDialog");
    }

    private void showInviteFriendsDialog() {
        // For now, just show a toast. In a real implementation, you'd show a dialog
        Toast.makeText(getContext(), "Invite Friends dialog would open here", Toast.LENGTH_SHORT).show();
    }

    private void leaveAlliance() {
        if (currentAlliance != null) {
            allianceService.leaveAlliance(currentAlliance.id, currentUserId)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Left alliance successfully", Toast.LENGTH_SHORT).show();
                    currentAlliance = null;
                    updateAllianceUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error leaving alliance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        }
    }

    private void disbandAlliance() {
        if (currentAlliance != null) {
            allianceService.disbandAlliance(currentAlliance.id, currentUserId)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Alliance disbanded successfully", Toast.LENGTH_SHORT).show();
                    currentAlliance = null;
                    updateAllianceUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error disbanding alliance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        }
    }

    @Override
    public void onAllianceCreated(String allianceId) {
        // Refresh the alliance data
        loadUserAlliance();
        Toast.makeText(getContext(), "Alliance created successfully!", Toast.LENGTH_SHORT).show();
    }
}
