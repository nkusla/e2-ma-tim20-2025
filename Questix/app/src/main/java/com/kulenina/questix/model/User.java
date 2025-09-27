package com.kulenina.questix.model;

import java.io.Serializable;

public class User implements IIdentifiable, Serializable {
	public String id;
	public String avatar;
	public String username;
	public String email;
	public Integer level;
	public Integer powerPoints;
	public Integer xp;
	public Integer coins;

	public User() {
		this.level = 1;
		this.powerPoints = 0;
		this.xp = 0;
		this.coins = 0;
	}

	public User(String id, String avatar, String username, String email, Integer level, Integer powerPoints, Integer xp, Integer coins) {
		this.id = id;
		this.avatar = avatar;
		this.username = username;
		this.email = email;
		this.level = level;
		this.powerPoints = powerPoints;
		this.xp = xp;
		this.coins = coins;
	}

	@Override
	public String getId() {
		return id;
	}
}