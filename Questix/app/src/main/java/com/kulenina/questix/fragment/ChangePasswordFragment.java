package com.kulenina.questix.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.kulenina.questix.R;
import com.kulenina.questix.databinding.FragmentChangePasswordBinding;
import com.kulenina.questix.service.AuthService;

public class ChangePasswordFragment extends Fragment {
    private FragmentChangePasswordBinding binding;
    private AuthService authService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_change_password, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authService = new AuthService();

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.buttonChangePassword.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String currentPassword = getTextFromEditText(binding.editTextCurrentPassword);
        String newPassword = getTextFromEditText(binding.editTextNewPassword);
        String confirmPassword = getTextFromEditText(binding.editTextConfirmPassword);

        clearErrors();

        if (!validateInputs(currentPassword, newPassword, confirmPassword)) {
            return;
        }

        showLoading(true);

        authService.changePassword(currentPassword, newPassword)
            .addOnSuccessListener(aVoid -> {
                showLoading(false);
                Toast.makeText(getContext(), "Password changed successfully!", Toast.LENGTH_LONG).show();
                clearFields();
            })
            .addOnFailureListener(e -> {
                showLoading(false);
                String errorMessage = getErrorMessage(e);
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
            });
    }

    private boolean validateInputs(String currentPassword, String newPassword, String confirmPassword) {
        boolean isValid = true;

        if (TextUtils.isEmpty(currentPassword)) {
            binding.textInputLayoutCurrentPassword.setError("Enter current password");
            isValid = false;
        }

        if (TextUtils.isEmpty(newPassword)) {
            binding.textInputLayoutNewPassword.setError("Enter new password");
            isValid = false;
        } else if (newPassword.length() < 6) {
            binding.textInputLayoutNewPassword.setError("New password must be at least 6 characters");
            isValid = false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            binding.textInputLayoutConfirmPassword.setError("Confirm new password");
            isValid = false;
        } else if (!newPassword.equals(confirmPassword)) {
            binding.textInputLayoutConfirmPassword.setError("Passwords do not match");
            isValid = false;
        }

        if (!TextUtils.isEmpty(currentPassword) && !TextUtils.isEmpty(newPassword) &&
            currentPassword.equals(newPassword)) {
            binding.textInputLayoutNewPassword.setError("New password must be different from current password");
            isValid = false;
        }

        return isValid;
    }

    private void clearErrors() {
        binding.textInputLayoutCurrentPassword.setError(null);
        binding.textInputLayoutNewPassword.setError(null);
        binding.textInputLayoutConfirmPassword.setError(null);
    }

    private void clearFields() {
        binding.editTextCurrentPassword.setText("");
        binding.editTextNewPassword.setText("");
        binding.editTextConfirmPassword.setText("");
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.buttonChangePassword.setEnabled(!show);

        binding.editTextCurrentPassword.setEnabled(!show);
        binding.editTextNewPassword.setEnabled(!show);
        binding.editTextConfirmPassword.setEnabled(!show);
    }

    private String getTextFromEditText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String getErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("The password is invalid")) {
                return "Current password is incorrect";
            } else if (message.contains("too many requests")) {
                return "Too many attempts. Please try again later";
            } else if (message.contains("network error")) {
                return "Network error. Please check your internet connection";
            }
        }
        return "Error changing password: " + (message != null ? message : "Unknown error");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
