package com.thefirstlineofcode.granite.framework.im;

import java.util.List;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;

public interface ISubscriptionService {
	List<Subscription> get(String user);
	Subscription get(String user, String contact);
	boolean exists(String user, String contact);
	void add(Subscription subscription);
	void updateNameAndGroups(String user, String contact, String name, String groups);
	void updateState(String user, String contact, Subscription.State state);
	void remove(String user, String contact);
	
	SubscriptionChanges handleSubscription(JabberId user, JabberId contact, SubscriptionType subscritionType);
	
	List<SubscriptionNotification> getNotificationsByUser(String user);
	List<SubscriptionNotification> getNotificationsByUserAndContact(String user, String contact);
	void addNotification(SubscriptionNotification notification);
	void removeNotification(SubscriptionNotification notification);
}
