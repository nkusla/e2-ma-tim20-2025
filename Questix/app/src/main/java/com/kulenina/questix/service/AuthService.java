package com.kulenina.questix.service;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.kulenina.questix.repository.UserRepository;
import com.kulenina.questix.model.User;

public class AuthService {
	private final FirebaseAuth mAuth;
	private final UserRepository userRepository;

	public AuthService() {
		mAuth = FirebaseAuth.getInstance();
		userRepository = new UserRepository();
	}

	public FirebaseUser getCurrentUser() {
		return mAuth.getCurrentUser();
	}

	public boolean isUserLoggedIn() {
		return getCurrentUser() != null;
	}

	public Task<AuthResult> signupUser(String email, String password, String username, String avatar) {
		return mAuth.createUserWithEmailAndPassword(email, password)
			.continueWithTask(authTask -> {
				if (authTask.isSuccessful()) {
					return createUserProfile(authTask, username, avatar);
				} else {
					throw authTask.getException();
				}
			});
	}

	private Task<AuthResult> createUserProfile(Task<AuthResult> authTask) {
		FirebaseUser user = mAuth.getCurrentUser();
		if (user == null) {
			throw new RuntimeException("User creation failed");
		}

		User userObj = new User();
		userObj.id = user.getUid();
		userObj.email = user.getEmail();
		userObj.username = user.getEmail();

		return userRepository.createWithId(userObj)
			.continueWithTask(repoTask -> {
				if (repoTask.isSuccessful()) {
					return authTask;
				} else {
					user.delete();
					throw repoTask.getException();
				}
			});
	}

	private Task<AuthResult> createUserProfile(Task<AuthResult> authTask, String username, String avatar) {
		FirebaseUser user = mAuth.getCurrentUser();
		if (user == null) {
			throw new RuntimeException("User creation failed");
		}

		User userObj = new User();
		userObj.id = user.getUid();
		userObj.email = user.getEmail();
		userObj.username = username;
		userObj.avatar = avatar;

		return userRepository.createWithId(userObj)
			.continueWithTask(repoTask -> {
				if (repoTask.isSuccessful()) {
					return authTask;
				} else {
					user.delete();
					throw repoTask.getException();
				}
			});
	}

		public Task<AuthResult> loginUser(String email, String password) {
			return mAuth.signInWithEmailAndPassword(email, password);
		}

	public void logout() {
		mAuth.signOut();
	}

	public Task<User> getCurrentUserProfile() {
		FirebaseUser firebaseUser = getCurrentUser();
		if (firebaseUser == null) {
			throw new RuntimeException("No user logged in");
		}
		return userRepository.read(firebaseUser.getUid());
	}
}
