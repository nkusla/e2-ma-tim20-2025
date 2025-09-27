package com.kulenina.questix.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.kulenina.questix.R;
import com.kulenina.questix.databinding.FragmentUserProfileBinding;
import com.kulenina.questix.model.User;
import com.kulenina.questix.viewmodel.UserViewModel;

public class UserProfileFragment extends Fragment {
    private FragmentUserProfileBinding binding;
    private UserViewModel userViewModel;

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
        binding.setViewModel(userViewModel);

        // Load user data from MainActivity or get it from arguments
        if (getArguments() != null) {
            User user = (User) getArguments().getSerializable("user");
            if (user != null) {
                userViewModel.setUser(user);
            }
        }
    }

    public void setUser(User user) {
        if (userViewModel != null) {
            userViewModel.setUser(user);
        }
    }
}
