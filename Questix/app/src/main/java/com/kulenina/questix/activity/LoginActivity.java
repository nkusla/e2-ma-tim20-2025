package com.kulenina.questix.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.kulenina.questix.R;
import com.kulenina.questix.service.AuthService;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
  private EditText editTextEmail, editTextPassword;
  private Button buttonLogin;
  private ProgressBar progressBar;
  private TextView textViewRegister;
  private AuthService authService;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    authService = new AuthService();

    editTextEmail = findViewById(R.id.editTextEmail);
    editTextPassword = findViewById(R.id.editTextPassword);
    buttonLogin = findViewById(R.id.buttonLogin);
    progressBar = findViewById(R.id.progressBar);
    textViewRegister = findViewById(R.id.textViewRegister);

    if (authService.isUserLoggedIn()) {
      startActivity(new Intent(LoginActivity.this, MainActivity.class));
      finish();
    }
  }

  public void onLoginClick(View view) {
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

    authService.loginUser(email, password)
      .addOnSuccessListener(authResult -> {
        showProgressBar(false);
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
      })
      .addOnFailureListener(e -> {
        showProgressBar(false);
        Toast.makeText(LoginActivity.this, "Invalid email or password", Toast.LENGTH_LONG).show();
      });
  }

  private void showProgressBar(boolean show) {
    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    buttonLogin.setEnabled(!show);
  }

  public void onRegisterClick(View view) {
    startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
    finish();
  }
}
