package com.thefirstlineofcode.granite.framework.im;

public class SubscriptionNotification {
	private String user;
	private String contact;
	private SubscriptionType subscriptionType;
	
	public void setUser(String user) {
		this.user = user;
	}
	
	public String getUser() {
		return user;
	}
	
	public void setContact(String contact) {
		this.contact = contact;
	}
	
	public String getContact() {
		return contact;
	}
	
	public SubscriptionType getSubscriptionType() {
		return subscriptionType;
	}
	
	public void setSubscriptionType(SubscriptionType subscriptionType) {
		this.subscriptionType = subscriptionType;
	}
	
}
