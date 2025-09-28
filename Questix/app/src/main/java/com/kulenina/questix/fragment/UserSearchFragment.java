package com.kulenina.questix.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.button.MaterialButtonToggleGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.kulenina.questix.R;
import com.kulenina.questix.databinding.FragmentUserSearchBinding;
import com.kulenina.questix.model.User;
import com.kulenina.questix.service.FriendshipService;
import com.kulenina.questix.adapter.UserAdapter;
import com.kulenina.questix.viewmodel.UserViewModel;
import com.kulenina.questix.service.AuthService;

import java.util.ArrayList;
import java.util.List;

public class UserSearchFragment extends Fragment implements UserAdapter.OnUserClickListener {
    private FragmentUserSearchBinding binding;
    private UserAdapter userAdapter;
    private FriendshipService friendshipService;
    private AuthService authService;
    private String currentUserId;
    private List<UserViewModel> allUserViewModels = new ArrayList<>();
    private List<UserViewModel> friendsViewModels = new ArrayList<>();
    private boolean isShowingFriends = true;
    private String currentSearchQuery = "";

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
        authService = new AuthService();
        currentUserId = authService.getCurrentUser().getUid();

        setupRecyclerView();
        setupToggleButtons();
        setupSearchInput();
        loadUsers();
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter(friendsViewModels, this);  // Start with friends list
        binding.recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewUsers.setAdapter(userAdapter);
    }

    private void setupToggleButtons() {
        // Set friends button as selected by default
        binding.buttonFriends.setChecked(true);

        // Handle toggle button changes
        binding.toggleGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (isChecked) {
                    if (checkedId == R.id.buttonFriends) {
                        switchToFriendsMode();
                    } else if (checkedId == R.id.buttonSearch) {
                        switchToSearchMode();
                    }
                }
            }
        });
    }

    private void setupSearchInput() {
        // Handle search button click
        binding.buttonDoSearch.setOnClickListener(v -> performSearch());

        // Handle enter key in search field
        binding.editTextSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    performSearch();
                    return true;
                }
                return false;
            }
        });
    }

    private void switchToFriendsMode() {
        isShowingFriends = true;
        binding.textViewTitle.setText("My Friends");
        binding.layoutSearch.setVisibility(View.GONE);
        userAdapter.updateUserList(friendsViewModels);
        loadFriends();
    }

    private void switchToSearchMode() {
        isShowingFriends = false;
        binding.textViewTitle.setText("Search Users");
        binding.layoutSearch.setVisibility(View.VISIBLE);
        currentSearchQuery = "";
        binding.editTextSearch.setText("");
        userAdapter.updateUserList(new ArrayList<>());
        updateUI();
    }

    private void performSearch() {
        String query = binding.editTextSearch.getText().toString().trim();
        currentSearchQuery = query;

        if (TextUtils.isEmpty(query)) {
            Toast.makeText(getContext(), "Please enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }

        searchUsers(query);
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

        // Load friends by default
        loadFriends();
    }

    private void loadFriends() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textViewNoUsers.setVisibility(View.GONE);

        friendshipService.getFriends(currentUserId)
            .addOnSuccessListener(friends -> {
                // Create UserViewModels for friends
                friendsViewModels.clear();

                for (User friend : friends) {
                    if (friend.id != null) {
                        UserViewModel userViewModel = new UserViewModel();
                        userViewModel.setUser(friend);
                        friendsViewModels.add(userViewModel);
                    }
                }

                binding.progressBar.setVisibility(View.GONE);
                if (isShowingFriends) {
                    userAdapter.updateUserList(friendsViewModels);
                }
                updateUI();
            })
            .addOnFailureListener(e -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.textViewNoUsers.setVisibility(View.VISIBLE);
                binding.textViewNoUsers.setText("Error loading friends: " + e.getMessage());
                Toast.makeText(getContext(), "Failed to load friends", Toast.LENGTH_SHORT).show();
            });
    }

    private void searchUsers(String query) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textViewNoUsers.setVisibility(View.GONE);

        loadAllUsers();
    }

    private void loadAllUsers() {
        friendshipService.getAllUsers()
            .addOnSuccessListener(users -> {
                // Filter out only the current user and create UserViewModels
                allUserViewModels.clear();
                List<UserViewModel> filteredUsers = new ArrayList<>();

                for (User user : users) {
                    if (user.id != null && !user.id.equals(currentUserId)) {
                        UserViewModel userViewModel = new UserViewModel();
                        userViewModel.setUser(user);
                        allUserViewModels.add(userViewModel);

                        // Apply search filter if in search mode
                        if (!isShowingFriends && !TextUtils.isEmpty(currentSearchQuery)) {
                            String username = userViewModel.getUsername().toLowerCase();
                            String title = userViewModel.getTitle().toLowerCase();
                            String query = currentSearchQuery.toLowerCase();

                            if (username.contains(query) || title.contains(query)) {
                                filteredUsers.add(userViewModel);
                            }
                        } else if (!isShowingFriends) {
                            filteredUsers.add(userViewModel);
                        }
                    }
                }

                binding.progressBar.setVisibility(View.GONE);

                if (!isShowingFriends) {
                    userAdapter.updateUserList(filteredUsers);
                }
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

        List<UserViewModel> currentList = isShowingFriends ? friendsViewModels :
            (userAdapter.getUserList() != null ? userAdapter.getUserList() : new ArrayList<>());

        if (currentList.isEmpty()) {
            binding.textViewNoUsers.setVisibility(View.VISIBLE);
            if (isShowingFriends) {
                binding.textViewNoUsers.setText("You don't have any friends yet");
            } else if (!TextUtils.isEmpty(currentSearchQuery)) {
                binding.textViewNoUsers.setText("No users found matching '" + currentSearchQuery + "'");
            } else {
                binding.textViewNoUsers.setText("Enter a search term to find users");
            }
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
