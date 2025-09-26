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

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseUser;
import com.kulenina.questix.R;
import com.kulenina.questix.service.AuthService;

public class MainActivity extends AppCompatActivity {
	private AuthService authHelper;
	private TextView textViewWelcome;
	private Button buttonLogout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_main);
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
			Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
			return insets;
		});

		// Initialize Firebase
		FirebaseApp.initializeApp(this);

		// Initialize Firebase Auth Helper
		authHelper = new AuthService();

		// Initialize views
		textViewWelcome = findViewById(R.id.textViewWelcome);
		buttonLogout = findViewById(R.id.buttonLogout);


		// Set up logout button
		buttonLogout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				authHelper.logout();
				Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
				startActivity(new Intent(MainActivity.this, LoginActivity.class));
				finish();
			}
		});

		// Check if user is logged in
		if (!authHelper.isUserLoggedIn()) {
			startActivity(new Intent(this, LoginActivity.class));
			finish();
			return;
		}

		// Display welcome message
		FirebaseUser user = authHelper.getCurrentUser();
		if (user != null) {
			textViewWelcome.setText("Welcome, " + user.getEmail() + "!");
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
			authHelper.logout();
			Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
			startActivity(new Intent(this, LoginActivity.class));
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}