package com.thefirstlineofcode.granite.im;

import java.util.ArrayList;
import java.util.List;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.BadRequest;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.Forbidden;
import com.thefirstlineofcode.basalt.xmpp.core.stream.error.NotAuthorized;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.granite.framework.core.annotations.AppComponent;
import com.thefirstlineofcode.granite.framework.core.annotations.BeanDependency;
import com.thefirstlineofcode.granite.framework.core.auth.IAuthenticator;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventFirer;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.framework.im.IResource;
import com.thefirstlineofcode.granite.framework.im.IResourcesService;
import com.thefirstlineofcode.granite.framework.im.ISubscriptionService;
import com.thefirstlineofcode.granite.framework.im.OfflineMessageEvent;
import com.thefirstlineofcode.granite.framework.im.Subscription;

@AppComponent("chat.message.deliverer")
public class ChatMessageDeliverer implements IServerConfigurationAware, IChatMessageDeliverer {
	
	@BeanDependency("authenticator")
	private IAuthenticator authenticator;
	
	@BeanDependency
	private IResourcesService resourcesService;
	
	@BeanDependency
	private ISubscriptionService subscriptionService;
	
	private String domain;
	
	@Override
	public boolean isMessageDeliverable(IProcessingContext context, Stanza stanza) {
		if ((stanza instanceof Message) && (((Message)stanza).getType()) == Message.Type.GROUPCHAT)
			return false;
		
		if (stanza.getTo() == null) {
			throw new ProtocolException(new BadRequest("A message should specify an intended recipient."));
		}
		
		JabberId from = stanza.getFrom() == null ? context.getJid() : stanza.getFrom();
		
		if (isToSelf(from, stanza.getTo())) {
			throw new ProtocolException(new BadRequest("Sending a message to yourself."));
		}
		
		if (!isToDomain(stanza.getTo())) {
			return false;
		}
		
		if (!authenticator.exists(stanza.getTo().getNode())) {
			throw new ProtocolException(new NotAuthorized());
		}
		
		if (!isSubscribed(from, stanza.getTo())) {
			throw new ProtocolException(new Forbidden(String.format("%s and %s didn't subscribe each other.",
					from, stanza.getTo())));
		}
		
		return true;
	}

	private boolean isSubscribed(JabberId from, JabberId to) {
		String user = from.getNode();
		String contact = to.getBareIdString();
		Subscription subscription = subscriptionService.get(user, contact);
		
		return Subscription.State.BOTH.equals(subscription.getState());
	}
	
	private boolean isToSelf(JabberId from, JabberId to) {
		return from.getBareIdString().equals(to.getBareIdString());
	}
	
	private boolean isToDomain(JabberId to) {
		return to.getDomain().equals(domain);
	}
	
	// Server Rules for Handling XML Stanzas(rfc3920 11)
	@Override
	public void deliver(IProcessingContext context, IEventFirer eventFirer, Message message) {
		JabberId to = message.getTo();
		if (to.getResource() != null) {
			IResource resoure = resourcesService.getResource(message.getTo());
			
			if (resoure != null && resoure.isAvailable()) {
				context.write(message);
				
				return;
			}
			
			to = to.getBareId();
		}
		
		IResource[] resources = resourcesService.getResources(to);
		if (resources.length == 0) {
			eventFirer.fire(new OfflineMessageEvent(context.getJid(), message.getTo(), message));
		} else {
			IResource[] chosen = chooseTargets(resources);
			if (chosen == null || chosen.length == 0) {
				eventFirer.fire(new OfflineMessageEvent(context.getJid(), message.getTo(), message));
			} else {
				for (IResource resource : chosen) {
					context.write(resource.getJid(), message);
				}
			}
		}
	}

	protected IResource[] chooseTargets(IResource[] resources) {
		List<IResource> lResources = new ArrayList<>();
		
		int priority = 0;
		
		for (IResource resource : resources) {
			if (!resource.isAvailable()) {
				continue;
			}
			
			int resourcePriority = 0;
			if (resource.getBroadcastPresence() != null && resource.getBroadcastPresence().getPriority() != null) {
				resourcePriority = resource.getBroadcastPresence().getPriority();
			}
			
			if(resourcePriority < priority) {
				continue;
			} else if (resourcePriority == priority) {
				lResources.add(resource);
			} else {
				lResources.clear();
				lResources.add(resource);
			}
		}
		
		return lResources.toArray(new IResource[lResources.size()]);
	}
	
	@Override
	public void setServerConfiguration(IServerConfiguration serverConfiguration) {
		this.domain = serverConfiguration.getDomainName();
	}
}
