package com.thefirstlineofcode.granite.lite.im;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.InternalServerError;
import com.thefirstlineofcode.granite.framework.core.adf.data.IDataObjectFactory;
import com.thefirstlineofcode.granite.framework.core.adf.data.IDataObjectFactoryAware;
import com.thefirstlineofcode.granite.framework.im.ISubscriptionService;
import com.thefirstlineofcode.granite.framework.im.Subscription;
import com.thefirstlineofcode.granite.framework.im.SubscriptionChanges;
import com.thefirstlineofcode.granite.framework.im.SubscriptionNotification;
import com.thefirstlineofcode.granite.framework.im.SubscriptionType;
import com.thefirstlineofcode.granite.im.SubscriptionStateChangeRules;

@Transactional
@Component
public class SubscriptionService implements ISubscriptionService, IDataObjectFactoryAware {
	@Autowired
	private SqlSession sqlSession;
	
	private IDataObjectFactory dataObjectFactory;
	
	@Override
	public List<Subscription> get(String user) {
		return getMapper().selectByUser(user);
	}

	@Override
	public Subscription get(String user, String contact) {
		return getMapper().selectByUserAndContact(user, contact);
	}

	@Override
	public boolean exists(String user, String contact) {
		return getMapper().selectCountByUserAndContact(user, contact) != 0;
	}

	private SubscriptionMapper getMapper() {
		return sqlSession.getMapper(SubscriptionMapper.class);
	}

	@Override
	public void add(Subscription subscription) {
		getMapper().insert(subscription);
	}

	@Override
	public void updateNameAndGroups(String user, String contact, String name, String groups) {
		getMapper().updateNameAndGroups(user, contact, name, groups);
	}
	
	@Override
	public void updateState(String user, String contact, Subscription.State state) {
		getMapper().updateState(user, contact, state);
	}
	
	@Override
	public SubscriptionChanges handleSubscription(JabberId user, JabberId contact,
			SubscriptionType subscriptionType) {
		SubscriptionChange userSubscriptionChange = handleOutboundSubscription(user, contact, subscriptionType);
		SubscriptionChange contactSubscriptionChange = handleInboundSubscription(contact, user, subscriptionType);
		
		return new SubscriptionChanges(
				userSubscriptionChange == null ? null : userSubscriptionChange.oldState,
				userSubscriptionChange == null ? null : userSubscriptionChange.subscription,
				contactSubscriptionChange == null ? null : contactSubscriptionChange.oldState,
				contactSubscriptionChange == null ? null : contactSubscriptionChange.subscription
		);
	}
	
	private class SubscriptionChange {
		public Subscription.State oldState;
		public Subscription subscription;
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
			subscription = dataObjectFactory.create(Subscription.class);
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

	@Override
	public void remove(String user, String contact) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<SubscriptionNotification> getNotificationsByUser(String user) {
		return getMapper().selectNotificationsByUser(user);
	}
	
	@Override
	public List<SubscriptionNotification> getNotificationsByUserAndContact(String user, String contact) {
		return getMapper().selectNotificationsByUserAndContact(user, contact);
	}

	@Override
	public void addNotification(SubscriptionNotification notification) {
		getMapper().insertNotification(notification);
	}

	@Override
	public void removeNotification(SubscriptionNotification notification) {
		getMapper().deleteNotification(notification);
	}

	@Override
	public void setDataObjectFactory(IDataObjectFactory dataObjectFactory) {
		this.dataObjectFactory = dataObjectFactory;
	}
}
