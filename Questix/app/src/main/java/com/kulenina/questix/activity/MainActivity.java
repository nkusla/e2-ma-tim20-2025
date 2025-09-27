package com.kulenina.questix.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import androidx.databinding.DataBindingUtil;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseUser;
import com.kulenina.questix.R;
import com.kulenina.questix.databinding.ActivityMainBinding;
import com.kulenina.questix.fragment.UserProfileFragment;
import com.kulenina.questix.fragment.QuestsFragment;
import com.kulenina.questix.model.User;
import com.kulenina.questix.service.AuthService;
import com.kulenina.questix.viewmodel.UserViewModel;
import com.kulenina.questix.fragment.UserSearchFragment;

public class MainActivity extends AppCompatActivity {
	private AuthService authService;
	private ActivityMainBinding binding;
	private UserViewModel userViewModel;
	private DrawerLayout drawerLayout;
	private ActionBarDrawerToggle drawerToggle;
	private NavigationView navigationView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);

		// Configure system UI for proper status bar appearance
		getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
				View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			);
		}

		binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
		// Handle system window insets for the main content area
		ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
			Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			// Apply top padding to main content to account for status bar
			v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
			return insets;
		});

		// Handle system window insets for navigation drawer
		ViewCompat.setOnApplyWindowInsetsListener(binding.navView, (v, insets) -> {
			Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			// Apply top padding to navigation view to account for status bar
			v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
			return insets;
		});

		authService = new AuthService();

		userViewModel = new UserViewModel();
		binding.setViewModel(userViewModel);
		binding.setActivity(this);

		// Set up toolbar
		setSupportActionBar(binding.toolbar);

		// Set up navigation drawer
		setupNavigationDrawer();

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
				showUserProfile();
			})
			.addOnFailureListener(e -> {
				populateWithFirebaseUserData(firebaseUser);
				userViewModel.setErrorMessage("Could not load profile data");
				showUserProfile();
			});
		}
	}

	private void setupNavigationDrawer() {
		drawerLayout = binding.drawerLayout;
		navigationView = binding.navView;

		drawerToggle = new ActionBarDrawerToggle(
			this, drawerLayout, binding.toolbar,
			R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawerLayout.addDrawerListener(drawerToggle);
		drawerToggle.syncState();

		navigationView.setNavigationItemSelectedListener(item -> {
			int itemId = item.getItemId();
			if (itemId == R.id.nav_profile) {
				showUserProfile();
			} else if (itemId == R.id.nav_quests) {
				showQuests();
			} else if (itemId == R.id.nav_search_users) {
				showUserSearch();
			} else if (itemId == R.id.nav_achievements) {
				// TODO: Implement achievements functionality
				Toast.makeText(this, "Achievements coming soon!", Toast.LENGTH_SHORT).show();
			} else if (itemId == R.id.nav_settings) {
				// TODO: Implement settings functionality
				Toast.makeText(this, "Settings coming soon!", Toast.LENGTH_SHORT).show();
			} else if (itemId == R.id.nav_logout) {
				authService.logout();
				Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
				startActivity(new Intent(this, LoginActivity.class));
				finish();
				return true;
			}
			drawerLayout.closeDrawer(navigationView);
			return true;
		});
	}

	private void showUserProfile() {
		User currentUser = userViewModel.getUser();
		if (currentUser != null && currentUser.id != null) {
			UserProfileFragment fragment = UserProfileFragment.newInstance(currentUser.id);
			replaceFragment(fragment);
		} else {
			// Fallback: show current user data if available
			UserProfileFragment fragment = new UserProfileFragment();
			replaceFragment(fragment);
		}
	}

	private void showQuests() {
		QuestsFragment fragment = new QuestsFragment();
		replaceFragment(fragment);
	}

	private void showUserSearch() {
		UserSearchFragment fragment = new UserSearchFragment();
		replaceFragment(fragment);
	}

	private void replaceFragment(Fragment fragment) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(R.id.fragment_container, fragment);
		transaction.commit();
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