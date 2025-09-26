package com.kulenina.questix.repository;

import com.kulenina.questix.model.User;

public class UserRepository extends Repository<User> {
	public UserRepository() {
		super("users", User.class);
	}
}