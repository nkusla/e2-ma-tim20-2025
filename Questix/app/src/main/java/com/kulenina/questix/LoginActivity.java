package com.kulenina.questix;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;

public class LoginActivity extends AppCompatActivity {
  private EditText editTextEmail, editTextPassword;
  private Button buttonLogin, buttonRegister;
  private ProgressBar progressBar;
  private AuthService authHelper;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    // Initialize Firebase
    FirebaseApp.initializeApp(this);

    // Initialize Firebase Auth Helper
    authHelper = new AuthService();

    // Initialize views
    editTextEmail = findViewById(R.id.editTextEmail);
    editTextPassword = findViewById(R.id.editTextPassword);
    buttonLogin = findViewById(R.id.buttonLogin);
    buttonRegister = findViewById(R.id.buttonRegister);
    progressBar = findViewById(R.id.progressBar);

    // Set click listeners
    buttonLogin.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        loginUser();
      }
    });

    buttonRegister.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        registerUser();
      }
    });

    if (authHelper.isUserLoggedIn()) {
      startActivity(new Intent(LoginActivity.this, MainActivity.class));
      finish();
    }
  }

  private void loginUser() {
    String email = editTextEmail.getText().toString().trim();
    String password = editTextPassword.getText().toString().trim();

    if (TextUtils.isEmpty(email)) {
      editTextEmail.setError("Email is required");
      editTextEmail.requestFocus();
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

    showProgressBar(true);

    authHelper.loginUser(email, password)
      .addOnSuccessListener(authResult -> {
        showProgressBar(false);
        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
      })
      .addOnFailureListener(e -> {
        showProgressBar(false);
        Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
      });
  }

  private void registerUser() {
    String email = editTextEmail.getText().toString().trim();
    String password = editTextPassword.getText().toString().trim();

    if (TextUtils.isEmpty(email)) {
      editTextEmail.setError("Email is required");
      editTextEmail.requestFocus();
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

    showProgressBar(true);

    authHelper.registerUser(email, password)
      .addOnSuccessListener(authResult -> {
        showProgressBar(false);
        Toast.makeText(LoginActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
      })
      .addOnFailureListener(e -> {
        showProgressBar(false);
        Toast.makeText(LoginActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
      });
  }

  private void showProgressBar(boolean show) {
    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    buttonLogin.setEnabled(!show);
    buttonRegister.setEnabled(!show);
  }
}
