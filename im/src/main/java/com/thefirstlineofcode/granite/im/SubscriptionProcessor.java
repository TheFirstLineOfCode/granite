package com.thefirstlineofcode.granite.im;

import java.util.List;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.BadRequest;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.FeatureNotImplemented;
import com.thefirstlineofcode.basalt.xmpp.im.roster.Item;
import com.thefirstlineofcode.basalt.xmpp.im.roster.Roster;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence.Type;
import com.thefirstlineofcode.granite.framework.core.adf.data.IDataObjectFactory;
import com.thefirstlineofcode.granite.framework.core.adf.data.IDataObjectFactoryAware;
import com.thefirstlineofcode.granite.framework.core.annotations.BeanDependency;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.auth.IAuthenticator;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.framework.core.utils.StanzaCloner;
import com.thefirstlineofcode.granite.framework.im.IPresenceProcessor;
import com.thefirstlineofcode.granite.framework.im.IResource;
import com.thefirstlineofcode.granite.framework.im.IResourcesService;
import com.thefirstlineofcode.granite.framework.im.ISubscriptionService;
import com.thefirstlineofcode.granite.framework.im.Subscription;
import com.thefirstlineofcode.granite.framework.im.SubscriptionChanges;
import com.thefirstlineofcode.granite.framework.im.SubscriptionNotification;
import com.thefirstlineofcode.granite.framework.im.SubscriptionType;
import com.thefirstlineofcode.granite.framework.im.Subscription.State;

public class SubscriptionProcessor implements IPresenceProcessor, IServerConfigurationAware,
		IDataObjectFactoryAware {
	
	@BeanDependency
	private ISubscriptionService subscriptionService;
	
	@BeanDependency("authenticator")
	private IAuthenticator authenticator;
	
	@Dependency("roster.operator")
	private RosterOperator rosterOperator;
	
	@BeanDependency
	private IResourcesService resourcesService;
	
	private IDataObjectFactory dataObjectFactory;
	
	private String domain;
	
	@Override
	public boolean process(IProcessingContext context, Presence presence) {
		if (!isSubscriptionPresence(presence))
			return false;
		
		return doProcess(context, presence);
	}

	protected boolean doProcess(IConnectionContext context, Presence presence) {
		JabberId user = getFrom(context, presence);
		JabberId contact = presence.getTo();
		
		checkUserAndContact(user, contact);
		
		processAcknowledgement(user, contact, presence);
		
		rosterSetIfNotExist(context, user, contact);
		
		SubscriptionChanges changes = subscriptionService.handleSubscription(user, contact,
				presenceTypeToSubscriptionType(presence.getType()));
		
		rosterPushIfChanged(context, user.getNode(), changes.getOldUserSubscriptionState(), changes.getUserSubscription());
		rosterPushIfChanged(context, contact.getNode(), changes.getOldContactSubscriptionState(), changes.getContactSubscription());
		
		deliverInboundSubscription(context, changes.getOldContactSubscriptionState(),
				changes.getContactSubscription(), presence);
		
		if (presence.getType() == Presence.Type.SUBSCRIBED &&
				isStateChanged(changes.getOldContactSubscriptionState(),
					changes.getContactSubscription() == null ? null : changes.getContactSubscription().getState())) {
			deliverAvailablePresence(context, user, contact);
		}
		
		if (presence.getType() == Presence.Type.UNSUBSCRIBE &&
				isStateChanged(changes.getOldContactSubscriptionState(),
					changes.getContactSubscription() == null ? null : changes.getContactSubscription().getState())) {
			deliverUnavailablePresence(context, user, contact);
		}
		
		return true;
	}
	
	private void deliverUnavailablePresence(IConnectionContext context, JabberId contact, JabberId user) {
		IResource[] contactResources = resourcesService.getResources(contact);
		IResource[] userResources = resourcesService.getResources(user);
		
		Presence unavailable = new Presence(Presence.Type.UNAVAILABLE);
		for (IResource contactResource : contactResources) {
			for (IResource userResource : userResources) {
				Presence cloned = StanzaCloner.clone(unavailable);
				cloned.setFrom(contactResource.getJid());
				cloned.setTo(userResource.getJid());
				
				context.write(cloned);
			}
		}
	}

	private SubscriptionType presenceTypeToSubscriptionType(Presence.Type type) {
		if (type == Presence.Type.SUBSCRIBE) {
			return SubscriptionType.SUBSCRIBE;
		} else if (type == Presence.Type.SUBSCRIBED) {
			return SubscriptionType.SUBSCRIBED;
		} else if (type == Presence.Type.UNSUBSCRIBE) {
			return SubscriptionType.UNSUBSCRIBE;
		} else {
			return SubscriptionType.UNSUBSCRIBED;
		}
	}

	private boolean processAcknowledgement(JabberId user, JabberId contact, Presence presence) {
		List<SubscriptionNotification> notifications = subscriptionService.getNotificationsByUserAndContact(
				user.getNode(), contact.getBareIdString());
		
		for (SubscriptionNotification notification : notifications) {
			if (isAcknowledgement(presence, notification.getSubscriptionType())) {
				subscriptionService.removeNotification(notification);
				return true;
			}
		}
		
		return false;
	}

	private boolean isAcknowledgement(Presence presence, SubscriptionType subscriptionType) {
		Presence.Type subscription = presence.getType();
		
		if (isSubscribeAcknowledge(subscriptionType, subscription) ||
				isUnsubscribeAcknowledge(subscriptionType, subscription) ||
					isSubscribedAcknowledge(subscriptionType, subscription) ||
						isUnsubscribedAcknowledge(subscriptionType, subscription)) {
			return true;
		}
		
		return false;
	}
	
	private boolean isSubscribeAcknowledge(SubscriptionType subscriptionType, Type subscription) {
		return (subscriptionType == SubscriptionType.SUBSCRIBE &&
				(subscription == Type.SUBSCRIBED ||
					subscription == Type.UNSUBSCRIBED));
	}
	
	private boolean isSubscribedAcknowledge(SubscriptionType subscriptionType, Type subscription) {
		return (subscriptionType == SubscriptionType.SUBSCRIBED &&
				(subscription == Type.SUBSCRIBE ||
					subscription == Type.UNSUBSCRIBE));
	}
	
	private boolean isUnsubscribeAcknowledge(SubscriptionType subscriptionType, Type subscription) {
		return (subscriptionType == SubscriptionType.UNSUBSCRIBE &&
				(subscription == Type.UNSUBSCRIBED ||
					subscription == Type.SUBSCRIBED));
	}

	private boolean isUnsubscribedAcknowledge(SubscriptionType subscriptionType, Type subscription) {
		return (subscriptionType == SubscriptionType.UNSUBSCRIBED &&
				(subscription == Type.UNSUBSCRIBE ||
					subscription == Type.SUBSCRIBE));
	}

	private void deliverAvailablePresence(IConnectionContext context, JabberId contact, JabberId user) {
		IResource[] contactResources = resourcesService.getResources(contact);
		IResource[] userResources = resourcesService.getResources(user);
		
		for (IResource contactResource : contactResources) {
			Presence presence = contactResource.getBroadcastPresence();
			if (presence == null)
				continue;
			
			for (IResource userResource : userResources) {	
				Presence available = StanzaCloner.clone(presence);
				
				available.setFrom(contactResource.getJid());
				available.setTo(userResource.getJid());
				
				context.write(available);
			}
		}
	}

	private boolean isStateChanged(State oldState, State newState) {
		if (oldState == null || newState == null)
			return false;
		
		return oldState != newState;
	}

	private void deliverInboundSubscription(IConnectionContext context, State oldContactState,
			Subscription contactSubscription, Presence presence) {
		if (oldContactState == null || contactSubscription == null)
			return;
		
		Presence.Type subscriptionType = presence.getType();
		
		if (subscriptionType == Presence.Type.SUBSCRIBE) {
			if (oldContactState == State.NONE ||
					oldContactState == State.NONE_PENDING_OUT ||
						oldContactState == State.TO) {
				deliverSubscriptionToContact(context, presence);
			} else if (oldContactState == State.FROM ||
					oldContactState == State.FROM_PENDING_OUT ||
					oldContactState == State.BOTH) {
				autoReply(context, presence);
			}
		} else if (subscriptionType == Presence.Type.UNSUBSCRIBE) {
			if (oldContactState == State.NONE_PENDING_IN ||
					oldContactState == State.NONE_PENDING_IN_OUT ||
						oldContactState == State.TO_PENDING_IN ||
							oldContactState == State.FROM ||
								oldContactState == State.FROM_PENDING_OUT ||
									oldContactState == State.BOTH) {
				deliverSubscriptionToContact(context, presence);
				autoReply(context, presence);
			}
		} else if (subscriptionType == Presence.Type.SUBSCRIBED) {
			if (oldContactState == State.NONE_PENDING_OUT ||
					oldContactState == State.NONE_PENDING_IN_OUT ||
						oldContactState == State.FROM_PENDING_OUT) {
				deliverSubscriptionToContact(context, presence);
			}
		} else { //subscriptionType == Presence.Type.UNSUBSCRIBED
			if (oldContactState == State.NONE_PENDING_OUT ||
					oldContactState == State.NONE_PENDING_IN_OUT ||
						oldContactState == State.TO ||
							oldContactState == State.TO_PENDING_IN ||
								oldContactState == State.FROM_PENDING_OUT ||
									oldContactState == State.BOTH) {
				deliverSubscriptionToContact(context, presence);
			}
		}
	}
	
	private void deliverSubscriptionToContact(IConnectionContext context, Presence presence) {
		SubscriptionType SubscriptionType = presenceTypeToSubscriptionType(presence.getType());
		saveNotification(presence.getTo().getNode(), context.getJid().getBareIdString(), SubscriptionType);
		
		IResource[] resources = resourcesService.getResources(presence.getTo());
		JabberId user = JabberId.parse(context.getJid().getBareIdString());
		for (IResource resource : resources) {
			Presence subscription = new Presence();
			subscription.setFrom(user);
			subscription.setTo(resource.getJid());
			subscription.setType(presence.getType());
			
			context.write(subscription);
		}
	}
	
	private void saveNotification(String user, String contact, SubscriptionType subscriptionType) {
		List<SubscriptionNotification> notifications = subscriptionService.getNotificationsByUserAndContact(user, contact);
		
		SubscriptionNotification existed = null;
		for (SubscriptionNotification notification : notifications) {
			if (notification.getSubscriptionType() == subscriptionType) {
				existed = notification;
			}
		}
		
		if (existed != null) {
			return;
		}
		
		SubscriptionNotification notification = dataObjectFactory.create(SubscriptionNotification.class);
		notification.setUser(user);
		notification.setContact(contact);
		notification.setSubscriptionType(subscriptionType);
		
		subscriptionService.addNotification(notification);
	}

	private void autoReply(IConnectionContext context, Presence presence) {
		IResource[] resources = resourcesService.getResources(context.getJid());
		JabberId contact = presence.getTo().getBareId();
		for (IResource resource : resources) {
			Presence subscription = new Presence();
			subscription.setFrom(contact);
			subscription.setTo(resource.getJid());
			subscription.setType(getAutoReplySubscriptionType(presence));
			
			context.write(subscription);
		}
	}

	private Type getAutoReplySubscriptionType(Presence presence) {
		if (presence.getType() == Type.SUBSCRIBE) {
			return Type.SUBSCRIBED;
		} else { // presence.getType() == Type.UNSUBSCRIBED
			return Type.UNSUBSCRIBED;
		}
	}

	private void rosterPushIfChanged(IConnectionContext context, String user,
			Subscription.State oldState, Subscription subscription) {
		if (oldState == null || subscription == null)
			return;
		
		if (!rosterItemStateChanged(oldState, subscription.getState()))
			return;
		
		Roster roster = rosterOperator.subscriptionToRoster(subscription);
		rosterOperator.rosterPush(context, user, roster);
	}
	
	private boolean rosterItemStateChanged(Subscription.State oldState, Subscription.State newState) {
		if (oldState == newState)
			return false;
		
		if (oldState == Subscription.State.NONE && newState == Subscription.State.NONE_PENDING_IN)
			return false;
		
		if (oldState == Subscription.State.NONE_PENDING_IN && newState == Subscription.State.NONE)
			return false;
		
		if (oldState == Subscription.State.TO && newState == Subscription.State.TO_PENDING_IN)
			return false;
		
		if (oldState == Subscription.State.TO_PENDING_IN && newState == Subscription.State.TO)
			return false;
		
		return true;
	}

	private void rosterSetIfNotExist(IConnectionContext context, JabberId user, JabberId contact) {
		Subscription subscription = subscriptionService.get(user.getNode(), contact.getBareIdString());
		if (subscription == null) {
			Item item = new Item();
			item.setJid(contact);
			item.setSubscription(Item.Subscription.NONE);
			
			Roster roster = new Roster();
			roster.addOrUpdate(item);
			
			rosterOperator.rosterSet(context, user, roster);
		}
	}

	private JabberId getFrom(IConnectionContext context, Presence presence) {
		JabberId from;
		if (context.getJid().getResource() == null) {
			from = presence.getFrom();
		} else {
			from = context.getJid();
		}
		
		return from;
	}

	private void checkUserAndContact(JabberId from, JabberId to) {
		if (from == null) {
			throw new ProtocolException(new BadRequest("Null subscription user."));
		}
		
		if (to == null) {
			throw new ProtocolException(new BadRequest("Null subscription contact."));
		}
		
		if (!domain.equals(from.getDomain()) || !domain.equals(to.getDomain())) {
			throw new ProtocolException(new FeatureNotImplemented("Feature S2S not implemented."));
		}
	}

	private boolean isSubscriptionPresence(Presence presence) {
		Type type = presence.getType();
		
		return (type == Type.SUBSCRIBE ||
			type == Type.SUBSCRIBED ||
			type == Type.UNSUBSCRIBE ||
			type == Type.UNSUBSCRIBED);
	}

	@Override
	public void setServerConfiguration(IServerConfiguration serverConfiguration) {
		this.domain = serverConfiguration.getDomainName();
	}

	@Override
	public void setDataObjectFactory(IDataObjectFactory dataObjectFactory) {
		this.dataObjectFactory = dataObjectFactory;
	}

}
