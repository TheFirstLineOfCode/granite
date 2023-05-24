package com.thefirstlineofcode.granite.cluster.im;

import java.util.List;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.InternalServerError;
import com.thefirstlineofcode.granite.framework.adf.mongodb.IDocToObj;
import com.thefirstlineofcode.granite.framework.adf.mongodb.IterableToList;
import com.thefirstlineofcode.granite.framework.im.ISubscriptionService;
import com.thefirstlineofcode.granite.framework.im.Subscription;
import com.thefirstlineofcode.granite.framework.im.Subscription.State;
import com.thefirstlineofcode.granite.framework.im.SubscriptionChanges;
import com.thefirstlineofcode.granite.framework.im.SubscriptionNotification;
import com.thefirstlineofcode.granite.framework.im.SubscriptionType;
import com.thefirstlineofcode.granite.im.SubscriptionStateChangeRules;

@Component
public class SubscriptionService implements ISubscriptionService {
	@Autowired
	private MongoDatabase database;

	@Override
	public List<Subscription> get(String user) {
		return new IterableToList<Subscription>().toList(
				getSubscriptionsCollection().find(Filters.eq("user", user)),
				new IDocToObj<Subscription>() {
					@Override
					public Subscription toObj(Document doc) {
						return docToSubscription(doc);
					}
				}
		);
	}

	private D_Subscription docToSubscription(Document doc) {
		D_Subscription subscription = new D_Subscription();
		subscription.setId(doc.getObjectId("_id").toHexString());
		subscription.setUser(doc.getString("user"));
		subscription.setContact(doc.getString("contact"));
		subscription.setState(State.valueOf(doc.getString("state")));
		subscription.setGroups(doc.getString("groups"));
		
		return subscription;
	}
	
	private MongoCollection<Document> getSubscriptionsCollection() {
		return database.getCollection("subscriptions");
	}

	@Override
	public Subscription get(String user, String contact) {
		Document doc = getSubscriptionsCollection().find(Filters.and(Filters.eq("user", user), Filters.eq("contact", contact))).first();
		
		if (doc == null)
			return null;
		
		return docToSubscription(doc);
	}

	@Override
	public boolean exists(String user, String contact) {
		return getSubscriptionsCollection().countDocuments(Filters.and(Filters.eq("user", user), Filters.eq("contact", contact))) == 1;
	}
	
	@Override
	public void add(Subscription subscription) {
		Document doc = new Document().
				append("user", subscription.getUser()).
				append("contact", subscription.getContact()).
				append("name", subscription.getName()).
				append("groups", subscription.getGroups()).
				append("state", subscription.getState().toString());
		
		getSubscriptionsCollection().insertOne(doc);
	}

	@Override
	public void updateNameAndGroups(String user, String contact, String name, String groups) {
		getSubscriptionsCollection().updateOne(Filters.and(Filters.eq("user", user), Filters.eq("contact", contact)),
				Updates.combine(Updates.set("name", name), Updates.set("groups", groups)));
	}

	@Override
	public void updateState(String user, String contact, State state) {
		getSubscriptionsCollection().updateOne(Filters.and(Filters.eq("user", user), Filters.eq("contact", contact)),
				Updates.set("state", state.toString()));
	}

	@Override
	public void remove(String user, String contact) {
		getSubscriptionsCollection().deleteOne(Filters.and(Filters.eq("name", user), Filters.eq("contact", contact)));
	}

	@Override
	public SubscriptionChanges handleSubscription(JabberId user, JabberId contact, SubscriptionType subscriptionType) {
		// TODO Use two phase commits to do multi-document transactions.
		SubscriptionChange userSubscriptionChange = null;
		SubscriptionChange contactSubscriptionChange = null;
		try {
			userSubscriptionChange = handleOutboundSubscription(user, contact, subscriptionType);
			contactSubscriptionChange = handleInboundSubscription(contact, user, subscriptionType);
		} catch (Exception e) {
			throw new ProtocolException(new InternalServerError("Can't handle subscription."), e);
		}
		
		return new SubscriptionChanges(
				userSubscriptionChange == null ? null : userSubscriptionChange.oldState,
				userSubscriptionChange == null ? null : userSubscriptionChange.subscription,
				contactSubscriptionChange == null ? null : contactSubscriptionChange.oldState,
				contactSubscriptionChange == null ? null : contactSubscriptionChange.subscription
		);
	}
	
	private SubscriptionChange handleOutboundSubscription(JabberId user, JabberId contact,
			SubscriptionType subscriptionType) {
		Subscription subscription = get(user.getNode(), contact.getBareIdString());
		if (subscription == null) {
			throw new ProtocolException(new InternalServerError("null subscription state. roster set first"));
		}
		
		Subscription.State oldState = subscription.getState();
		Subscription.State newState = SubscriptionStateChangeRules.getOutboundSubscriptionNewState(oldState, subscriptionType);
		
		if (newState == oldState)
			return null;
		
		subscription.setState(newState);
		updateState(user.getNode(), contact.getBareIdString(), newState);
		
		SubscriptionChange change = new SubscriptionChange();
		change.oldState = oldState;
		change.subscription = subscription;
		
		return change;
	}

	private SubscriptionChange handleInboundSubscription(JabberId user, JabberId contact, SubscriptionType subscriptionType) {
		boolean subscriptionExist = true;
		Subscription subscription = get(user.getNode(), contact.getBareIdString());
		
		if (subscription == null) {
			subscriptionExist = false;
			subscription = new D_Subscription();
			subscription.setUser(user.getNode());
			subscription.setContact(contact.getBareIdString());
			subscription.setState(Subscription.State.NONE);
		}
		
		Subscription.State oldState = subscription.getState();
		Subscription.State newState = SubscriptionStateChangeRules.getInboundSubscriptionNewState(oldState, subscriptionType);
		
		if (newState == oldState)
			return null;
		
		subscription.setState(newState);
		if (subscriptionExist) {
			updateState(user.getNode(), contact.getBareIdString(), newState);
		} else {
			add(subscription);
		}
		
		SubscriptionChange change = new SubscriptionChange();
		change.oldState = oldState;
		change.subscription = subscription;
		
		return change;
	}
	
	private class SubscriptionChange {
		public Subscription.State oldState;
		public Subscription subscription;
	}

	@Override
	public List<SubscriptionNotification> getNotificationsByUser(String user) {
		return new IterableToList<SubscriptionNotification>().toList(
				getSubscriptionNotificationsCollection().find(Filters.eq("user", user)),
				new IDocToObj<SubscriptionNotification>() {
					@Override
					public SubscriptionNotification toObj(Document doc) {
						return docToSubscriptionNotification(doc);
					}
				}
		);
	}

	private SubscriptionNotification docToSubscriptionNotification(Document doc) {
		SubscriptionNotification notification = new SubscriptionNotification();
		notification.setUser(doc.getString("user"));
		notification.setContact(doc.getString("contact"));
		notification.setSubscriptionType(SubscriptionType.valueOf(doc.getString("subscription_type")));
		
		return notification;
	}

	@Override
	public List<SubscriptionNotification> getNotificationsByUserAndContact(String user, String contact) {
		return new IterableToList<SubscriptionNotification>().toList(
				getSubscriptionNotificationsCollection().find(Filters.eq("user", user)),
				new IDocToObj<SubscriptionNotification>() {
					@Override
					public SubscriptionNotification toObj(Document doc) {
						return docToSubscriptionNotification(doc);
					}
				}
		);
	}

	@Override
	public void addNotification(SubscriptionNotification notification) {
		Document doc = new Document().
				append("user", notification.getUser()).
				append("contact", notification.getContact()).
				append("subscription_type", notification.getSubscriptionType().toString());
		getSubscriptionNotificationsCollection().insertOne(doc);
	}

	@Override
	public void removeNotification(SubscriptionNotification notification) {
		getSubscriptionNotificationsCollection().deleteOne(
				Filters.and(
						Filters.eq("user", notification.getUser()),
						Filters.eq("contact", notification.getContact()),
						Filters.eq("subscription_type", notification.getSubscriptionType().toString())
				)
		);
	}
	
	private MongoCollection<Document> getSubscriptionNotificationsCollection() {
		return database.getCollection("subscription_notifications");
	}

}
