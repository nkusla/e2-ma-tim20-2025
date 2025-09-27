package com.kulenina.questix.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.databinding.DataBindingUtil;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseUser;
import com.kulenina.questix.R;
import com.kulenina.questix.databinding.ActivityMainBinding;
import com.kulenina.questix.model.User;
import com.kulenina.questix.service.AuthService;
import com.kulenina.questix.viewmodel.UserViewModel;

public class MainActivity extends AppCompatActivity {
	private AuthService authService;
	private ActivityMainBinding binding;
	private UserViewModel userViewModel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);

		binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
		ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
			Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
			return insets;
		});

		FirebaseApp.initializeApp(this);

		authService = new AuthService();


		userViewModel = new UserViewModel();
		binding.setViewModel(userViewModel);
		binding.setActivity(this);

		if (!authService.isUserLoggedIn()) {
			startActivity(new Intent(this, LoginActivity.class));
			finish();
			return;
		}

		FirebaseUser firebaseUser = authService.getCurrentUser();
		if (firebaseUser != null) {
		authService.getCurrentUserProfile()
			.addOnSuccessListener(user -> {
				if (user != null) {
					userViewModel.setUser(user);
				} else {
					populateWithFirebaseUserData(firebaseUser);
				}
			})
			.addOnFailureListener(e -> {
				populateWithFirebaseUserData(firebaseUser);
				userViewModel.setErrorMessage("Could not load profile data");
			});
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_logout) {
			authService.logout();
			Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
			startActivity(new Intent(this, LoginActivity.class));
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void populateWithFirebaseUserData(FirebaseUser firebaseUser) {
		User fallbackUser = new User();
		fallbackUser.email = firebaseUser.getEmail();
		fallbackUser.username = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Not set";
		fallbackUser.level = 1;
		fallbackUser.xp = 0;
		fallbackUser.coins = 0;
		fallbackUser.powerPoints = 0;

		userViewModel.setUser(fallbackUser);
	}

	public void onLogoutClick(View view) {
		authService.logout();
		Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
		startActivity(new Intent(MainActivity.this, LoginActivity.class));
		finish();
	}
}