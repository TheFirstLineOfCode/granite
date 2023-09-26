package com.thefirstlineofcode.granite.cluster.im;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.InternalServerError;
import com.thefirstlineofcode.granite.framework.im.ISubscriptionService;
import com.thefirstlineofcode.granite.framework.im.Subscription;
import com.thefirstlineofcode.granite.framework.im.Subscription.State;
import com.thefirstlineofcode.granite.framework.im.SubscriptionChanges;
import com.thefirstlineofcode.granite.framework.im.SubscriptionNotification;
import com.thefirstlineofcode.granite.framework.im.SubscriptionType;
import com.thefirstlineofcode.granite.im.SubscriptionStateChangeRules;

@Component
@Transactional
public class SubscriptionService implements ISubscriptionService {
	private static final String FIELD_NAME_SUBSCRIPTION_TYPE = "subscriptionType";
	private static final String FIELD_NAME_STATE = "state";
	private static final String FIELD_NAME_GROUPS = "groups";
	private static final String FIELD_NAME_NAME = "name";
	private static final String FIELD_NAME_CONTACT = "contact";
	private static final String FIELD_NAME_USER = "user";
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	@Override
	public List<Subscription> get(String user) {
		List<D_Subscription> subscriptions = mongoTemplate.find(
				new Query().addCriteria(Criteria.where(FIELD_NAME_USER).is(user)),
				D_Subscription.class);
		
		return new ArrayList<Subscription>(subscriptions);
	}
	
	@Override
	public Subscription get(String user, String contact) {
		return mongoTemplate.findOne(
				new Query().addCriteria(Criteria.where(FIELD_NAME_USER).is(user).and(FIELD_NAME_CONTACT).is(contact)),
				D_Subscription.class);
	}

	@Override
	public boolean exists(String user, String contact) {
		return mongoTemplate.count(
				new Query().addCriteria(Criteria.where(FIELD_NAME_USER).is(user).and(FIELD_NAME_CONTACT).is(contact)),
				D_Subscription.class) != 0;
	}
	
	@Override
	public void add(Subscription subscription) {
		if (!(subscription instanceof D_Subscription)) {
			throw new IllegalArgumentException(String.format("Must be type of '%s'", D_Subscription.class.getName()));
		}
		
		mongoTemplate.save(subscription);
	}

	@Override
	public void updateNameAndGroups(String user, String contact, String name, String groups) {
		Query query = new Query().addCriteria(Criteria.where(FIELD_NAME_USER).is(user).and(FIELD_NAME_CONTACT).is(contact));
		
		Update update = new Update();
		update.set(FIELD_NAME_NAME, name);
		update.set(FIELD_NAME_GROUPS, groups);
		
		mongoTemplate.updateFirst(query, update, D_Subscription.class);
	}

	@Override
	public void updateState(String user, String contact, State state) {
		Query query = new Query().addCriteria(Criteria.where(FIELD_NAME_USER).is(user).and(FIELD_NAME_CONTACT).is(contact));
		
		Update update = new Update();
		update.set(FIELD_NAME_STATE, state.toString());
		
		mongoTemplate.updateFirst(query, update, D_Subscription.class);
	}

	@Override
	public void remove(String user, String contact) {
		Query query = new Query().addCriteria(Criteria.where(FIELD_NAME_USER).is(user).and(FIELD_NAME_CONTACT).is(contact));
		mongoTemplate.remove(query, D_Subscription.class);
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
		List<D_SubscriptionNotification> notifications = mongoTemplate.find(
				new Query().addCriteria(Criteria.where(FIELD_NAME_USER).is(user)),
				D_SubscriptionNotification.class);
		return new ArrayList<SubscriptionNotification>(notifications);
	}
	
	@Override
	public List<SubscriptionNotification> getNotificationsByUserAndContact(String user, String contact) {
		List<D_SubscriptionNotification> notifications = mongoTemplate.find(
				new Query().addCriteria(Criteria.where(FIELD_NAME_USER).is(user).and(FIELD_NAME_CONTACT).is(contact)),
				D_SubscriptionNotification.class);
		return new ArrayList<SubscriptionNotification>(notifications);
	}

	@Override
	public void addNotification(SubscriptionNotification notification) {
		mongoTemplate.save(notification);
	}

	@Override
	public void removeNotification(SubscriptionNotification notification) {
		Query query = new Query().addCriteria(
				Criteria.where(FIELD_NAME_USER).is(notification.getUser()).
				and(FIELD_NAME_CONTACT).is(notification.getContact()).
				and(FIELD_NAME_SUBSCRIPTION_TYPE).is(notification.getSubscriptionType()));
		mongoTemplate.remove(query, D_SubscriptionNotification.class);
	}
}
