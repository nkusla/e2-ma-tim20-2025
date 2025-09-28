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
import com.kulenina.questix.fragment.AllianceListFragment;
import com.kulenina.questix.fragment.AllianceInvitationFragment;
import com.kulenina.questix.fragment.QRScannerFragment;
import com.kulenina.questix.service.AuthService;
import com.kulenina.questix.fragment.UserSearchFragment;

public class MainActivity extends AppCompatActivity {
	private AuthService authService = new AuthService();

	private ActivityMainBinding binding;
	private DrawerLayout drawerLayout;
	private ActionBarDrawerToggle drawerToggle;
	private NavigationView navigationView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);

		getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
				View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			);
		}

		binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
		ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
			Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
			return insets;
		});

		ViewCompat.setOnApplyWindowInsetsListener(binding.navView, (v, insets) -> {
			Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
			return insets;
		});

		binding.setActivity(this);

		setSupportActionBar(binding.toolbar);

		setupNavigationDrawer();

		if (!authService.isUserLoggedIn()) {
			startActivity(new Intent(this, LoginActivity.class));
			finish();
			return;
		}

		showUserProfile();
	}

	private void setupNavigationDrawer() {
		drawerLayout = binding.drawerLayout;
		navigationView = binding.navView;

		drawerToggle = new ActionBarDrawerToggle(
			this, drawerLayout, binding.toolbar,
			R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawerLayout.addDrawerListener(drawerToggle);
		drawerToggle.syncState();

		navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);
	}

	private boolean onNavigationItemSelected(android.view.MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.nav_profile) {
			showUserProfile();
		} else if (itemId == R.id.nav_alliances) {
			showAlliances();
		} else if (itemId == R.id.nav_search_users) {
			showUserSearch();
		} else if (itemId == R.id.nav_qr_scanner) {
			showQRScanner();
		} else if (itemId == R.id.nav_invites) {
			showInvites();
		} else if (itemId == R.id.nav_logout) {
			logout();
		}
		drawerLayout.closeDrawer(navigationView);
		return true;
	}

	private void showUserProfile() {
		FirebaseUser currentUser = authService.getCurrentUser();
		if (currentUser != null) {
			UserProfileFragment fragment = UserProfileFragment.newInstance(currentUser.getUid());
			replaceFragment(fragment);
		}
	}

	private void showUserSearch() {
		UserSearchFragment fragment = new UserSearchFragment();
		replaceFragment(fragment);
	}

	private void showAlliances() {
		AllianceListFragment fragment = new AllianceListFragment();
		replaceFragment(fragment);
	}

	private void showQRScanner() {
		QRScannerFragment fragment = new QRScannerFragment();
		replaceFragment(fragment);
	}

	private void showInvites() {
		AllianceInvitationFragment fragment = new AllianceInvitationFragment();
		replaceFragment(fragment);
	}

	private void replaceFragment(Fragment fragment) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(R.id.fragment_container, fragment);
		transaction.commit();
	}


	public void logout() {
		authService.logout();
		Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
		startActivity(new Intent(MainActivity.this, LoginActivity.class));
		finish();
	}
}