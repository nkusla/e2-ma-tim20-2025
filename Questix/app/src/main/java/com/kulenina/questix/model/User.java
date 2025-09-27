package com.kulenina.questix.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements IIdentifiable, Serializable {
	public String id;
	public String avatar;
	public String username;
	public String email;
	public Integer level;
	public Integer powerPoints;
	public Integer xp;
	public Integer coins;
	public List<String> friends; // List of friend user IDs

	public User() {
		this.level = 1;
		this.powerPoints = 0;
		this.xp = 0;
		this.coins = 0;
		this.friends = new ArrayList<>();
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
		this.friends = new ArrayList<>();
	}

	@Override
	public String getId() {
		return id;
	}

	public void addFriend(String friendId) {
		if (friends == null) {
			friends = new ArrayList<>();
		}
		if (!friends.contains(friendId)) {
			friends.add(friendId);
		}
	}

	public void removeFriend(String friendId) {
		if (friends != null) {
			friends.remove(friendId);
		}
	}

	public boolean isFriend(String friendId) {
		return friends != null && friends.contains(friendId);
	}
}