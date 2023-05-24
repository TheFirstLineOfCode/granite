package com.thefirstlineofcode.granite.framework.im;

public class Subscription {
	public enum State {
		NONE,
		NONE_PENDING_OUT,
		NONE_PENDING_IN,
		NONE_PENDING_IN_OUT,
		TO,
		TO_PENDING_IN,
		FROM,
		FROM_PENDING_OUT,
		BOTH
	}
	
	private String user;
	private String contact;
	private String name;
	private State state;
	private String groups;
	
	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getContact() {
		return contact;
	}

	public void setContact(String contact) {
		this.contact = contact;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public String getGroups() {
		return groups;
	}

	public void setGroups(String groups) {
		this.groups = groups;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
