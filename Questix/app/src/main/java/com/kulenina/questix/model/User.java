package com.kulenina.questix.model;

public class User implements IIdentifiable {
	public String id;
	public String username;
	public String email;

	public User() {
	}

	public User(String id, String username, String email) {
		this.id = id;
		this.username = username;
		this.email = email;
	}

	@Override
	public String getId() {
		return id;
	}
}