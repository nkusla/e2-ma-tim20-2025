package com.kulenina.questix.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.kulenina.questix.R;
import com.kulenina.questix.databinding.FragmentUserSearchBinding;
import com.kulenina.questix.model.User;
import com.kulenina.questix.service.FriendshipService;
import com.kulenina.questix.adapter.UserAdapter;
import com.kulenina.questix.viewmodel.UserViewModel;

import java.util.ArrayList;
import java.util.List;

public class UserSearchFragment extends Fragment implements UserAdapter.OnUserClickListener {
    private FragmentUserSearchBinding binding;
    private UserAdapter userAdapter;
    private FriendshipService friendshipService;
    private String currentUserId;
    private List<UserViewModel> allUserViewModels = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_search, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        friendshipService = new FriendshipService();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupRecyclerView();
        loadUsers();
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter(allUserViewModels, this);
        binding.recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewUsers.setAdapter(userAdapter);
    }

    private void loadUsers() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textViewNoUsers.setVisibility(View.GONE);

        if (currentUserId == null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.textViewNoUsers.setVisibility(View.VISIBLE);
            binding.textViewNoUsers.setText("User not logged in");
            return;
        }

        // Always load all users (excluding current user)
        loadAllUsers();
    }

    private void loadAllUsers() {
        friendshipService.getAllUsers()
            .addOnSuccessListener(users -> {
                // Filter out only the current user and create UserViewModels
                allUserViewModels.clear();

                for (User user : users) {
                    if (user.id != null && !user.id.equals(currentUserId)) {
                        UserViewModel userViewModel = new UserViewModel();
                        userViewModel.setUser(user);
                        allUserViewModels.add(userViewModel);
                    }
                }

                binding.progressBar.setVisibility(View.GONE);
                updateUI();
            })
            .addOnFailureListener(e -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.textViewNoUsers.setVisibility(View.VISIBLE);
                binding.textViewNoUsers.setText("Error loading users: " + e.getMessage());
                Toast.makeText(getContext(), "Failed to load users", Toast.LENGTH_SHORT).show();
            });
    }

    private void updateUI() {
        userAdapter.notifyDataSetChanged();

        if (allUserViewModels.isEmpty()) {
            binding.textViewNoUsers.setVisibility(View.VISIBLE);
            binding.textViewNoUsers.setText("No users found");
        } else {
            binding.textViewNoUsers.setVisibility(View.GONE);
        }
    }

    @Override
    public void onUserClick(User user) {
        // Navigate to user profile
        if (getActivity() != null) {
            UserProfileFragment profileFragment = UserProfileFragment.newInstance(user.id);
            getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, profileFragment)
                .addToBackStack(null)
                .commit();
        }
    }
}
