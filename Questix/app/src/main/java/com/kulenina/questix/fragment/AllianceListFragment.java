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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.kulenina.questix.R;
import com.kulenina.questix.databinding.FragmentAllianceListBinding;
import com.kulenina.questix.model.Alliance;
import com.kulenina.questix.service.AllianceService;
import com.kulenina.questix.service.AuthService;
import com.kulenina.questix.dialog.CreateAllianceDialog;

public class AllianceListFragment extends Fragment implements CreateAllianceDialog.OnAllianceCreatedListener, AllianceInvitationFragment.OnInvitationUpdateListener {
    private FragmentAllianceListBinding binding;
    private AllianceService allianceService;
    private AuthService authService;
    private String currentUserId;
    private Alliance currentAlliance;
    private AllianceInvitationFragment invitationFragment;

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
        authService = new AuthService();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupUI();
        loadUserAlliance();
        setupInvitationFragment();
    }

    private void setupUI() {
        binding.buttonCreateAlliance.setOnClickListener(v -> showCreateAllianceDialog());
        binding.buttonLeaveAlliance.setOnClickListener(v -> leaveAlliance());
        binding.buttonDisbandAlliance.setOnClickListener(v -> disbandAlliance());

        // Initialize UI state - show loading by default
        showLoadingState();
    }

    private void setupInvitationFragment() {
        invitationFragment = new AllianceInvitationFragment();
        invitationFragment.setOnInvitationUpdateListener(this);

        getChildFragmentManager().beginTransaction()
            .replace(R.id.fragment_container_invitations, invitationFragment)
            .commit();
    }

    private void loadUserAlliance() {
        allianceService.getUserAlliance(currentUserId)
            .addOnSuccessListener(alliance -> {
                currentAlliance = alliance;
                updateAllianceUI();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error loading alliance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                currentAlliance = null;
                updateAllianceUI();
            });
    }


    private void showLoadingState() {
        binding.layoutLoading.setVisibility(View.VISIBLE);
        binding.layoutNoAlliance.setVisibility(View.GONE);
        binding.layoutAllianceInfo.setVisibility(View.GONE);
        binding.fragmentContainerInvitations.setVisibility(View.GONE);
    }

    private void updateAllianceUI() {
        // Hide loading state
        binding.layoutLoading.setVisibility(View.GONE);

        if (currentAlliance != null) {
            binding.textViewAllianceName.setText(currentAlliance.name);
            binding.textViewMemberCount.setText("Members: " + currentAlliance.getMemberCount());
            binding.textViewMissionStatus.setText(currentAlliance.isMissionActive ? "Mission Active" : "No Active Mission");

            // Show/hide buttons based on user role and alliance status
            binding.buttonLeaveAlliance.setVisibility(currentAlliance.canLeave(currentUserId) ? View.VISIBLE : View.GONE);
            binding.buttonDisbandAlliance.setVisibility(currentAlliance.canDisband(currentUserId) ? View.VISIBLE : View.GONE);

            binding.layoutAllianceInfo.setVisibility(View.VISIBLE);
            binding.layoutNoAlliance.setVisibility(View.GONE);
        } else {
            binding.layoutAllianceInfo.setVisibility(View.GONE);
            binding.layoutNoAlliance.setVisibility(View.VISIBLE);
        }

        // Update invitations UI
        updateInvitationsUI();
    }

    private void updateInvitationsUI() {
        // Only update invitations if we're not in loading state
        if (binding.layoutLoading.getVisibility() != View.VISIBLE) {
            if (invitationFragment != null && invitationFragment.hasInvitations()) {
                binding.fragmentContainerInvitations.setVisibility(View.VISIBLE);
            } else {
                binding.fragmentContainerInvitations.setVisibility(View.GONE);
            }
        }
    }

    private void showCreateAllianceDialog() {
        CreateAllianceDialog dialog = CreateAllianceDialog.newInstance(this);
        dialog.show(getParentFragmentManager(), "CreateAllianceDialog");
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

    @Override
    public void onInvitationAccepted() {
        // Refresh alliance data when invitation is accepted
        loadUserAlliance();
        updateInvitationsUI();
    }

    @Override
    public void onInvitationDeclined() {
        // Update invitations UI when invitation is declined
        updateInvitationsUI();
    }
}
