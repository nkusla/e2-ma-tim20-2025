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
import com.kulenina.questix.databinding.FragmentUserProfileBinding;
import com.kulenina.questix.model.User;
import com.kulenina.questix.service.AuthService;
import com.kulenina.questix.service.FriendshipService;
import com.kulenina.questix.viewmodel.UserViewModel;

public class UserProfileFragment extends Fragment {
    private FragmentUserProfileBinding binding;
    private UserViewModel userViewModel;
    private AuthService authService;

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
        binding.setViewModel(userViewModel);

        if (getArguments() != null) {
            String userId = getArguments().getString("userId");
            if (userId != null) {
                fetchUserById(userId);
                setupFriendButton(userId);
            }
        }
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

    public void setUser(User user) {
        if (userViewModel != null) {
            userViewModel.setUser(user);
        }
    }

    private void setupFriendButton(String profileUserId) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Don't show friend button for own profile
        if (currentUserId.equals(profileUserId)) {
            return;
        }

        FriendshipService friendshipService = new FriendshipService();

        // Check if already friends
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
                setupFriendButton(friendId); // Refresh button
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
                setupFriendButton(friendId); // Refresh button
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
