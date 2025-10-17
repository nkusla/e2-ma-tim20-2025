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
	public List<String> friends;
	public String currentAllianceId;
	public List<String> allianceInvitations;
	public String fcmToken;

	public User() {
		this.level = 0;
		this.powerPoints = 0;
		this.xp = 0;
		this.coins = 0;
		this.friends = new ArrayList<>();
		this.allianceInvitations = new ArrayList<>();
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
		this.allianceInvitations = new ArrayList<>();
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

	public void setCurrentAlliance(String allianceId) {
		this.currentAllianceId = allianceId;
	}

	public void leaveAlliance() {
		this.currentAllianceId = null;
	}

	public boolean isInAlliance() {
		return currentAllianceId != null && !currentAllianceId.isEmpty();
	}

	public void addAllianceInvitation(String invitationId) {
		if (allianceInvitations == null) {
			allianceInvitations = new ArrayList<>();
		}
		if (!allianceInvitations.contains(invitationId)) {
			allianceInvitations.add(invitationId);
		}
	}

	public void removeAllianceInvitation(String invitationId) {
		if (allianceInvitations != null) {
			allianceInvitations.remove(invitationId);
		}
	}

	public boolean hasPendingInvitations() {
		return allianceInvitations != null && !allianceInvitations.isEmpty();
	}

	public int getPendingInvitationCount() {
		return allianceInvitations != null ? allianceInvitations.size() : 0;
	}

	public void setFcmToken(String fcmToken) {
		this.fcmToken = fcmToken;
	}

	public String getFcmToken() {
		return fcmToken;
	}

	public boolean hasFcmToken() {
		return fcmToken != null && !fcmToken.isEmpty();
	}
}