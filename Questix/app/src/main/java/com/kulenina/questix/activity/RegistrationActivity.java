package com.kulenina.questix.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.kulenina.questix.R;
import com.kulenina.questix.service.AuthService;

public class RegistrationActivity extends AppCompatActivity {
    private EditText editTextEmail, editTextUsername, editTextPassword, editTextConfirmPassword;
    private Button buttonRegister;
    private ProgressBar progressBar;
    private TextView textViewLogin;
    private ImageView[] avatars;
    private AuthService authService;
    private String selectedAvatar = "avatar_1"; // Default avatar
    private int selectedAvatarIndex = 0; // Track selected avatar index

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        authService = new AuthService();

        initializeViews();

        setAvatarClickListeners();

        avatars[selectedAvatarIndex].setBackgroundResource(R.drawable.avatar_border_selected);
    }

    private void initializeViews() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        progressBar = findViewById(R.id.progressBar);
        textViewLogin = findViewById(R.id.textViewLogin);

        avatars = new ImageView[5];
        avatars[0] = findViewById(R.id.avatar1);
        avatars[1] = findViewById(R.id.avatar2);
        avatars[2] = findViewById(R.id.avatar3);
        avatars[3] = findViewById(R.id.avatar4);
        avatars[4] = findViewById(R.id.avatar5);
    }

    public void onRegisterClick(View view) {
        registerUser();
    }

    public void onLoginClick(View view) {
        startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
        finish();
    }

    private void setAvatarClickListeners() {
        for (int i = 0; i < avatars.length; i++) {
            final int avatarIndex = i;
            avatars[i].setOnClickListener(v -> selectAvatar(avatarIndex));
        }
    }

    private void selectAvatar(int avatarIndex) {
        for (ImageView avatar : avatars) {
            avatar.setBackgroundResource(R.drawable.avatar_border_unselected);
        }

        selectedAvatarIndex = avatarIndex;
        selectedAvatar = "avatar_" + (avatarIndex + 1);
        avatars[avatarIndex].setBackgroundResource(R.drawable.avatar_border_selected);
    }

    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Please enter a valid email address");
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(username)) {
            editTextUsername.setError("Username is required");
            editTextUsername.requestFocus();
            return;
        }

        if (username.length() < 3) {
            editTextUsername.setError("Username must be at least 3 characters");
            editTextUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters");
            editTextPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            editTextConfirmPassword.setError("Please confirm your password");
            editTextConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Passwords do not match");
            editTextConfirmPassword.requestFocus();
            return;
        }

        showProgressBar(true);

        authService.signupUser(email, password, username, selectedAvatar)
            .addOnSuccessListener(authResult -> {
                showProgressBar(false);
                Toast.makeText(RegistrationActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                finish();
            })
            .addOnFailureListener(e -> {
                showProgressBar(false);
                String errorMessage = "Registration failed";
                Toast.makeText(RegistrationActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            });
    }

    private void showProgressBar(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        buttonRegister.setEnabled(!show);
    }
}
