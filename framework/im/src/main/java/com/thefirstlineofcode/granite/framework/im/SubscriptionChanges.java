package com.thefirstlineofcode.granite.framework.im;

public class SubscriptionChanges {
	private Subscription.State oldUserSubscriptionState;
	private Subscription userSubscription;
	private Subscription.State oldContactSubscriptionState;
	private Subscription contactSubscription;
	
	public SubscriptionChanges(Subscription.State oldUserSubscription, Subscription userSubscription,
			Subscription.State oldContactState, Subscription contactSubscription) {
		this.oldUserSubscriptionState = oldUserSubscription;
		this.userSubscription = userSubscription;
		this.oldContactSubscriptionState = oldContactState;
		this.contactSubscription = contactSubscription;
	}
	
	public Subscription.State getOldUserSubscriptionState() {
		return oldUserSubscriptionState;
	}

	public void setOldUserSubscriptionState(Subscription.State oldUserSubscriptionState) {
		this.oldUserSubscriptionState = oldUserSubscriptionState;
	}

	public Subscription getUserSubscription() {
		return userSubscription;
	}

	public void setUserSubscription(Subscription userSubscription) {
		this.userSubscription = userSubscription;
	}

	public Subscription.State getOldContactSubscriptionState() {
		return oldContactSubscriptionState;
	}

	public void setOldContactSubscriptionState(Subscription.State oldContactSubscriptionState) {
		this.oldContactSubscriptionState = oldContactSubscriptionState;
	}

	public Subscription getContactSubscription() {
		return contactSubscription;
	}

	public void setContactSubscription(Subscription contactSubscription) {
		this.contactSubscription = contactSubscription;
	}
	
}
