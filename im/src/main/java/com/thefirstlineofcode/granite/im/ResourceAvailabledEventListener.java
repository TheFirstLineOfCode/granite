package com.thefirstlineofcode.granite.im;

import java.util.List;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence.Type;
import com.thefirstlineofcode.granite.framework.core.annotations.BeanDependency;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventListener;
import com.thefirstlineofcode.granite.framework.im.ISubscriptionService;
import com.thefirstlineofcode.granite.framework.im.ResourceAvailabledEvent;
import com.thefirstlineofcode.granite.framework.im.SubscriptionNotification;
import com.thefirstlineofcode.granite.framework.im.SubscriptionType;

public class ResourceAvailabledEventListener implements IEventListener<ResourceAvailabledEvent> {
	@BeanDependency
	private ISubscriptionService subscriptionService;

	@Override
	public void process(IEventContext context, ResourceAvailabledEvent event) {
		JabberId user = event.getJid();
		List<SubscriptionNotification> notifications = subscriptionService.getNotificationsByUser(user.getNode());
		
		for (SubscriptionNotification notification : notifications) {
			Presence subscription = new Presence();
			subscription.setFrom(JabberId.parse(notification.getContact()));
			subscription.setTo(user);
			
			subscription.setType(subscriptionTypeToPresenceType(notification.getSubscriptionType()));
			
			context.write(subscription);
		}
	}

	private Type subscriptionTypeToPresenceType(SubscriptionType subscriptionType) {
		if (subscriptionType == SubscriptionType.SUBSCRIBE) {
			return Presence.Type.SUBSCRIBE;
		} else if (subscriptionType == SubscriptionType.UNSUBSCRIBE) {
			return Presence.Type.UNSUBSCRIBE;
		} else if (subscriptionType == SubscriptionType.SUBSCRIBED) {
			return Presence.Type.SUBSCRIBED;
		} else { // subscriptionType == SubscriptionTypee.UNSUBSCRIBED
			return Presence.Type.UNSUBSCRIBED;
		}
	}

}
