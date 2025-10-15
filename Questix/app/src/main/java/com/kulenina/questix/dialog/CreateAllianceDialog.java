package com.kulenina.questix.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.kulenina.questix.R;
import com.kulenina.questix.adapter.FriendSelectionAdapter;
import com.kulenina.questix.model.User;
import com.kulenina.questix.service.AllianceService;
import com.kulenina.questix.service.AllianceInvitationService;
import com.kulenina.questix.service.FriendshipService;
import com.kulenina.questix.viewmodel.UserViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CreateAllianceDialog extends DialogFragment implements FriendSelectionAdapter.OnFriendSelectionChangeListener {
    private EditText editTextAllianceName;
    private RecyclerView recyclerViewFriends;
    private Button buttonCreate;
    private Button buttonCancel;
    private ProgressBar progressBar;
    private TextView textViewNoFriends;
    private TextView textViewSelectedCount;

    private FriendSelectionAdapter friendAdapter;
    private List<UserViewModel> friendViewModels;
    private Set<String> selectedFriendIds;
    private AllianceService allianceService;
    private AllianceInvitationService invitationService;
    private FriendshipService friendshipService;
    private String currentUserId;

    public interface OnAllianceCreatedListener {
        void onAllianceCreated(String allianceId);
    }

    private OnAllianceCreatedListener listener;

    public static CreateAllianceDialog newInstance(OnAllianceCreatedListener listener) {
        CreateAllianceDialog dialog = new CreateAllianceDialog();
        dialog.listener = listener;
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogTheme);
        friendViewModels = new ArrayList<>();
        selectedFriendIds = new HashSet<>();
        allianceService = new AllianceService();
        invitationService = new AllianceInvitationService();
        friendshipService = new FriendshipService();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle("Create Alliance");
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_create_alliance, container, false);

        initViews(view);
        setupRecyclerView();
        loadFriends();

        return view;
    }

    private void initViews(View view) {
        editTextAllianceName = view.findViewById(R.id.editTextAllianceName);
        recyclerViewFriends = view.findViewById(R.id.recyclerViewFriends);
        buttonCreate = view.findViewById(R.id.buttonCreate);
        buttonCancel = view.findViewById(R.id.buttonCancel);
        progressBar = view.findViewById(R.id.progressBar);
        textViewNoFriends = view.findViewById(R.id.textViewNoFriends);
        textViewSelectedCount = view.findViewById(R.id.textViewSelectedCount);

        buttonCreate.setOnClickListener(v -> createAlliance());
        buttonCancel.setOnClickListener(v -> dismiss());
    }

    private void setupRecyclerView() {
        friendAdapter = new FriendSelectionAdapter(friendViewModels, this);
        recyclerViewFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewFriends.setAdapter(friendAdapter);
    }

    private void loadFriends() {
        progressBar.setVisibility(View.VISIBLE);
        textViewNoFriends.setVisibility(View.GONE);
        recyclerViewFriends.setVisibility(View.GONE);

        friendshipService.getFriends(currentUserId)
            .addOnSuccessListener(friends -> {
                progressBar.setVisibility(View.GONE);

                if (friends == null || friends.isEmpty()) {
                    textViewNoFriends.setVisibility(View.VISIBLE);
                    textViewNoFriends.setText("You don't have any friends to invite");
                } else {
                    // Convert friends to UserViewModels
                    friendViewModels.clear();
                    for (User friend : friends) {
                        UserViewModel userViewModel = new UserViewModel();
                        userViewModel.setUser(friend);
                        friendViewModels.add(userViewModel);
                    }

                    friendAdapter.notifyDataSetChanged();
                    recyclerViewFriends.setVisibility(View.VISIBLE);
                    updateSelectedCount();
                }
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                textViewNoFriends.setVisibility(View.VISIBLE);
                textViewNoFriends.setText("Error loading friends: " + e.getMessage());
            });
    }

    private void createAlliance() {
        String allianceName = editTextAllianceName.getText().toString().trim();

        if (TextUtils.isEmpty(allianceName)) {
            editTextAllianceName.setError("Alliance name is required");
            editTextAllianceName.requestFocus();
            return;
        }

        if (allianceName.length() < 3) {
            editTextAllianceName.setError("Alliance name must be at least 3 characters");
            editTextAllianceName.requestFocus();
            return;
        }

        if (allianceName.length() > 30) {
            editTextAllianceName.setError("Alliance name must be less than 30 characters");
            editTextAllianceName.requestFocus();
            return;
        }

        buttonCreate.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        // Create alliance
        allianceService.createAlliance(allianceName, currentUserId)
            .addOnSuccessListener(allianceId -> {
                // Send invitations to selected friends
                if (!selectedFriendIds.isEmpty()) {
                    sendInvitationsToFriends(allianceId, allianceName);
                } else {
                    // No friends selected, just show success
                    progressBar.setVisibility(View.GONE);
                    buttonCreate.setEnabled(true);
                    if (listener != null) {
                        listener.onAllianceCreated(allianceId);
                    }
                    dismiss();
                }
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                buttonCreate.setEnabled(true);
                Toast.makeText(getContext(), "Error creating alliance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void sendInvitationsToFriends(String allianceId, String allianceName) {
        List<String> friendIds = new ArrayList<>(selectedFriendIds);
        int totalInvitations = friendIds.size();
        final int[] completedInvitations = {0};

        for (String friendId : friendIds) {
            invitationService.createInvitation(allianceId, currentUserId, friendId)
                .addOnSuccessListener(invitationId -> {
                    completedInvitations[0]++;
                    if (completedInvitations[0] == totalInvitations) {
                        // All invitations sent
                        progressBar.setVisibility(View.GONE);
                        buttonCreate.setEnabled(true);
                        if (listener != null) {
                            listener.onAllianceCreated(allianceId);
                        }
                        dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    completedInvitations[0]++;
                    if (completedInvitations[0] == totalInvitations) {
                        // All invitations processed (some may have failed)
                        progressBar.setVisibility(View.GONE);
                        buttonCreate.setEnabled(true);
                        if (listener != null) {
                            listener.onAllianceCreated(allianceId);
                        }
                        dismiss();
                    }
                });
        }
    }

    @Override
    public void onSelectionChanged(Set<String> selectedFriendIds) {
        this.selectedFriendIds = selectedFriendIds;
        updateSelectedCount();
    }

    private void updateSelectedCount() {
        int count = selectedFriendIds.size();
        if (count == 0) {
            textViewSelectedCount.setText("No friends selected");
        } else if (count == 1) {
            textViewSelectedCount.setText("1 friend selected");
        } else {
            textViewSelectedCount.setText(count + " friends selected");
        }
    }
}
