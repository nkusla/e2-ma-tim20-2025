package com.kulenina.questix.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.kulenina.questix.R;
import com.kulenina.questix.databinding.FragmentAllianceListBinding;
import com.kulenina.questix.adapter.AllianceMessageAdapter;
import com.kulenina.questix.model.Alliance;
import com.kulenina.questix.model.AllianceMessage;
import com.kulenina.questix.service.AllianceService;
import com.kulenina.questix.service.AuthService;
import com.kulenina.questix.dialog.CreateAllianceDialog;

import java.util.List;

public class AllianceListFragment extends Fragment implements CreateAllianceDialog.OnAllianceCreatedListener {
    private FragmentAllianceListBinding binding;
    private AllianceService allianceService;
    private AuthService authService;
    private String currentUserId;
    private Alliance currentAlliance;

    // Messaging components
    private AllianceMessageAdapter messageAdapter;
    private RecyclerView messagesRecyclerView;
    private EditText messageEditText;

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
        setupMessaging();
        loadUserAlliance();
    }

    private void setupUI() {
        binding.buttonCreateAlliance.setOnClickListener(v -> showCreateAllianceDialog());
        binding.buttonLeaveAlliance.setOnClickListener(v -> leaveAlliance());
        binding.buttonDisbandAlliance.setOnClickListener(v -> disbandAlliance());

        showLoadingState();
    }

    private void setupMessaging() {
        messageAdapter = new AllianceMessageAdapter(currentUserId);
        messagesRecyclerView = binding.recyclerViewMessages;
        messageEditText = binding.editTextMessage;

        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        messagesRecyclerView.setAdapter(messageAdapter);

        binding.buttonSendMessage.setOnClickListener(v -> sendMessage());
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
        binding.layoutAllianceChat.setVisibility(View.GONE);
    }

    private void updateAllianceUI() {
        binding.layoutLoading.setVisibility(View.GONE);

        if (currentAlliance != null) {
            binding.textViewAllianceName.setText(currentAlliance.name);
            binding.textViewMemberCount.setText("Members: " + currentAlliance.getMemberCount());
            binding.textViewMissionStatus.setText(currentAlliance.isMissionActive ? "Mission Active" : "No Active Mission");

            binding.buttonLeaveAlliance.setVisibility(currentAlliance.canLeave(currentUserId) ? View.VISIBLE : View.GONE);
            binding.buttonDisbandAlliance.setVisibility(currentAlliance.canDisband(currentUserId) ? View.VISIBLE : View.GONE);

            binding.layoutAllianceInfo.setVisibility(View.VISIBLE);
            binding.layoutAllianceChat.setVisibility(View.VISIBLE);
            binding.layoutNoAlliance.setVisibility(View.GONE);

            loadAllianceMessages();
        } else {
            binding.layoutAllianceInfo.setVisibility(View.GONE);
            binding.layoutAllianceChat.setVisibility(View.GONE);
            binding.layoutNoAlliance.setVisibility(View.VISIBLE);
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
        loadUserAlliance();
    }


    private void loadAllianceMessages() {
        if (currentAlliance != null) {
            allianceService.getAllianceMessages(currentAlliance.id)
                .addOnSuccessListener(messages -> {
                    messageAdapter.updateMessages(messages);
                    if (!messages.isEmpty()) {
                        messagesRecyclerView.scrollToPosition(messages.size() - 1);
                    }
                })
                .addOnFailureListener(e -> {
                    String errorMessage = "Error loading messages";
                    if (e.getMessage() != null) {
                        if (e.getMessage().contains("FAILED_PRECONDITION")) {
                            errorMessage = "Database configuration issue. Please try again later.";
                        } else if (e.getMessage().contains("PERMISSION_DENIED")) {
                            errorMessage = "You don't have permission to view these messages.";
                        } else if (e.getMessage().contains("UNAVAILABLE")) {
                            errorMessage = "Server is temporarily unavailable. Please try again.";
                        } else {
                            errorMessage = "Error loading messages: " + e.getMessage();
                        }
                    }
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                });
        }
    }

    private void sendMessage() {
        if (currentAlliance == null) {
            return;
        }

        String messageText = messageEditText.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        messageEditText.setText("");

        allianceService.sendMessage(currentAlliance.id, currentUserId, messageText)
            .addOnSuccessListener(messageId -> {
                loadAllianceMessages();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error sending message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                messageEditText.setText(messageText);
            });
    }
}
