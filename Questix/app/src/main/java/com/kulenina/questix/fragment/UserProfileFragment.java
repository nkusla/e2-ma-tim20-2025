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
import com.kulenina.questix.adapter.EquipmentDisplayAdapter;
import com.kulenina.questix.databinding.FragmentUserProfileBinding;
import com.kulenina.questix.model.User;
import com.kulenina.questix.service.AuthService;
import com.kulenina.questix.service.EquipmentService;
import com.kulenina.questix.service.FriendshipService;
import com.kulenina.questix.viewmodel.UserViewModel;

public class UserProfileFragment extends Fragment {
    private FragmentUserProfileBinding binding;
    private UserViewModel userViewModel;
    private AuthService authService;
    private EquipmentService equipmentService;
    private EquipmentDisplayAdapter equipmentAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_profile, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userViewModel = new UserViewModel();
        authService = new AuthService();
        equipmentService = new EquipmentService();
        binding.setViewModel(userViewModel);

        setupRecyclerView();

        showLoadingState();

        if (getArguments() != null) {
            String userId = getArguments().getString("userId");
            if (userId != null) {
                String currentUserId = authService.getCurrentUser().getUid();
                userViewModel.setIsOwnProfile(currentUserId.equals(userId));

                fetchUserById(userId);
                fetchActiveEquipment(userId);
                setupFriendButton(userId);
            }
        }
    }

    private void setupRecyclerView() {
        equipmentAdapter = new EquipmentDisplayAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.recyclerViewActiveEquipment.setLayoutManager(layoutManager);
        binding.recyclerViewActiveEquipment.setAdapter(equipmentAdapter);
    }

    private void showLoadingState() {
        userViewModel.setIsLoading(true);
        userViewModel.setErrorMessage(null);
    }

    private void fetchUserById(String userId) {
        userViewModel.setIsLoading(true);
        userViewModel.setErrorMessage(null);

        authService.getUser(userId)
            .addOnSuccessListener(user -> {
                userViewModel.setIsLoading(false);
                if (user != null) {
                    userViewModel.setUser(user);
                } else {
                    userViewModel.setErrorMessage("User not found");
                }
            })
            .addOnFailureListener(e -> {
                userViewModel.setIsLoading(false);
                userViewModel.setErrorMessage("Failed to load user: " + e.getMessage());
            });
    }

    private void fetchActiveEquipment(String userId) {
        equipmentService.getActiveEquipment(userId)
            .addOnSuccessListener(activeEquipment -> {
                userViewModel.setActiveEquipment(activeEquipment);
                equipmentAdapter.setEquipmentList(activeEquipment);
            })
            .addOnFailureListener(e -> {
                userViewModel.setActiveEquipment(new java.util.ArrayList<>());
                equipmentAdapter.setEquipmentList(new java.util.ArrayList<>());
            });
    }

    public void setUser(User user) {
        if (userViewModel != null) {
            userViewModel.setIsLoading(false);
            userViewModel.setErrorMessage(null);
            userViewModel.setUser(user);
        }
    }

    private void setupFriendButton(String profileUserId) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (currentUserId.equals(profileUserId)) {
            return;
        }

        FriendshipService friendshipService = new FriendshipService();

        friendshipService.areFriends(currentUserId, profileUserId)
            .addOnSuccessListener(areFriends -> {
                Button friendButton = getView().findViewById(R.id.buttonAddFriend);
                if (friendButton != null) {
                    if (areFriends) {
                        friendButton.setText("Remove Friend");
                        friendButton.setOnClickListener(v -> removeFriend(profileUserId));
                    } else {
                        friendButton.setText("Add Friend");
                        friendButton.setOnClickListener(v -> addFriend(profileUserId));
                    }
                    friendButton.setVisibility(View.VISIBLE);
                }
            });
    }

    private void addFriend(String friendId) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FriendshipService friendshipService = new FriendshipService();

        friendshipService.addFriend(currentUserId, friendId)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Friend added!", Toast.LENGTH_SHORT).show();
                setupFriendButton(friendId);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void removeFriend(String friendId) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FriendshipService friendshipService = new FriendshipService();

        friendshipService.removeFriend(currentUserId, friendId)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Friend removed!", Toast.LENGTH_SHORT).show();
                setupFriendButton(friendId);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    public static UserProfileFragment newInstance(String userId) {
        UserProfileFragment fragment = new UserProfileFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        fragment.setArguments(args);
        return fragment;
    }
}
