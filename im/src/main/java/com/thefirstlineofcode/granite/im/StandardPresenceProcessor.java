package com.thefirstlineofcode.granite.im;

import java.util.List;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.FeatureNotImplemented;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.NotAllowed;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.UnexpectedRequest;
import com.thefirstlineofcode.basalt.xmpp.core.stream.error.InternalServerError;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence.Type;
import com.thefirstlineofcode.granite.framework.core.annotations.BeanDependency;
import com.thefirstlineofcode.granite.framework.core.auth.IAuthenticator;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventFirer;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventFirerAware;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.framework.core.utils.StanzaCloner;
import com.thefirstlineofcode.granite.framework.im.IPresenceProcessor;
import com.thefirstlineofcode.granite.framework.im.IResource;
import com.thefirstlineofcode.granite.framework.im.IResourcesRegister;
import com.thefirstlineofcode.granite.framework.im.IResourcesService;
import com.thefirstlineofcode.granite.framework.im.ISubscriptionService;
import com.thefirstlineofcode.granite.framework.im.ResourceAvailableEvent;
import com.thefirstlineofcode.granite.framework.im.Subscription;
import com.thefirstlineofcode.granite.framework.im.Subscription.State;

public class StandardPresenceProcessor implements IPresenceProcessor, IEventFirerAware {
	@BeanDependency("authenticator")
	private IAuthenticator authenticator;
	
	@BeanDependency
	private IResourcesService resourcesService;
	
	@BeanDependency
	private IResourcesRegister resourcesRegister;
	
	@BeanDependency
	private ISubscriptionService subscriptionService;
	
	private IEventFirer eventFirer;

	@Override
	public boolean process(IProcessingContext context, Presence presence) {
		if (presence.getType() != null &&
				presence.getType() != Type.UNAVAILABLE &&
					presence.getType() != Type.PROBE)
			return false;
		
		JabberId to = presence.getTo();
		if (to != null && !isInstantMessagingUser(to)) {
			return false;
		}
		
		JabberId from = presence.getFrom();
		if (from == null) {
			presence.setFrom(context.getJid());
		}
		
		if (!presence.getFrom().equals(context.getJid())) {
			throw new ProtocolException(new NotAllowed(String.format("'from' attribute should be %s.", context.getJid())));
		}
		
		return doProcess(context, presence);
	}

	protected boolean doProcess(IProcessingContext context, Presence presence) {
		JabberId user = context.getJid();
		
		IResource resource = resourcesService.getResource(user);
		if (resource == null) {
			throw new ProtocolException(new InternalServerError(String.format("Resource %s doesn't exist.", user)));
		}
		
		if (!resource.isAvailable()) {
			if (presence.getTo() != null || presence.getType() != null) {
				throw new ProtocolException(new UnexpectedRequest("Expect a initial presence."));
			}
			
			// process initial presence
			processInitialPresence(context, user, presence);
			return true;
		}
		
		if (isBroadcastPresence(user, presence)) {
			processBroadcastPresence(context, user, presence);
		} else if (isProbePresence(presence)) {
			processProbePresence(context, presence);
		} else if (isDirectedPresence(presence)) {
			processDirectedPresence(context, presence);
		} else {
			return false;
		}
		
		return true;
	}
	
	private void processInitialPresence(IProcessingContext context, JabberId user, Presence presence) {
		try {
			resourcesRegister.setBroadcastPresence(user, presence);
		} catch (Exception e) {
			throw new ProtocolException(new InternalServerError("Can't set resource's initial presence.", e));
		}
		
		try {
			resourcesRegister.setAvailable(user);
		} catch (Exception e) {
			throw new ProtocolException(new InternalServerError("Can't set resource to be available.", e));
		}
		
		eventFirer.fire(new ResourceAvailableEvent(user));
		
		List<Subscription> subscriptions = subscriptionService.get(user.getNode());
		for (Subscription subscription : subscriptions) {
			boolean toState = isToState(subscription);
			boolean fromState = isFromState(subscription);
			
			if (!toState && !fromState)
				continue;
			
			JabberId contact = JabberId.parse(subscription.getContact());
			IResource[] contactResources = resourcesService.getResources(contact);
			
			if (toState) {
				probeContact(context, user, contactResources);
			}
			
			if (fromState) {
				sendPresenceToContact(context, presence, user, contactResources);
			}
		}
		
		sendPresenceToUserOtherAvailableResources(context, user, presence);
		probeUserOtherAvailableResources(context, user);
	}

	private void probeUserOtherAvailableResources(IProcessingContext context, JabberId user) {
		IResource[] userResources = resourcesService.getResources(user);
		
		for (IResource resource : userResources) {
			if (resource.getJid().equals(user))
				continue;
			
			if (!resource.isAvailable())
				continue;
			
			Presence availiability = StanzaCloner.clone(resource.getBroadcastPresence());
			
			availiability.setFrom(resource.getJid());
			availiability.setTo(user);
			
			context.write(availiability);					
		}
	}

	private void sendPresenceToContact(IProcessingContext context, Presence presence, JabberId user,
				IResource[] contactResources) {
		for (IResource resource : contactResources) {
			if (!resource.isAvailable())
				continue;
			
			Presence availiability = StanzaCloner.clone(presence);
			
			availiability.setFrom(user);
			availiability.setTo(resource.getJid());
			
			context.write(availiability);					
		}
	}

	private void sendPresenceToUserOtherAvailableResources(IProcessingContext context,
				JabberId user, Presence presence) {
		IResource[] userResources = resourcesService.getResources(user);
		
		for (IResource resource : userResources) {
			if (resource.getJid().equals(user))
				continue;
			
			if (!resource.isAvailable())
				continue;
			
			Presence availiability = StanzaCloner.clone(presence);
			
			availiability.setFrom(user);
			availiability.setTo(resource.getJid());
			
			context.write(availiability);					
		}
	}

	private void probeContact(IProcessingContext context, JabberId user,
			IResource[] contactResources) {
		for (IResource resource : contactResources) {
			if (!resource.isAvailable())
				continue;
			
			Presence availiability = StanzaCloner.clone(resource.getBroadcastPresence());
			
			availiability.setFrom(resource.getJid());
			availiability.setTo(user);
			
			context.write(availiability);
		}
	}
	
	private boolean isFromState(Subscription subscription) {
		State state = subscription.getState();
		return state == State.FROM || state == State.FROM_PENDING_OUT || state == State.BOTH;
	}

	private boolean isToState(Subscription subscription) {
		State state = subscription.getState();		
		return state == State.TO || state == State.TO_PENDING_IN || state == State.BOTH;
	}

	private boolean isBroadcastPresence(JabberId user, Presence presence) {
		if (presence.getTo() != null ||
				(presence.getType() != Presence.Type.UNAVAILABLE &&
					presence.getType() != null))
			return false;
		
		return true;
	}
	
	private void processBroadcastPresence(IProcessingContext context, JabberId user, Presence presence) {
		try {
			resourcesRegister.setBroadcastPresence(user, presence);
		} catch (Exception e) {
			throw new ProtocolException(new InternalServerError("Can't set resource's broadcast presence.", e));
		}
		
		List<Subscription> subscriptions = subscriptionService.get(user.getNode());
		for (Subscription subscription : subscriptions) {
			boolean fromState = isFromState(subscription);
			
			if (!fromState)
				continue;
			
			JabberId contact = JabberId.parse(subscription.getContact());
			IResource[] contactResources = resourcesService.getResources(contact);
			
			sendPresenceToContact(context, presence, user, contactResources);
		}
		
		sendPresenceToUserOtherAvailableResources(context, user, presence);
	}
	
	private boolean isProbePresence(Presence presence) {
		return presence.getType() == Presence.Type.PROBE;
	}

	private void processProbePresence(IProcessingContext context, Presence presence) {
		// TODO
		throw new ProtocolException(new FeatureNotImplemented("Feature presence probe isn't implemented yet."));
	}
	
	private boolean isDirectedPresence(Presence presence) {
		return presence.getTo() != null;
	}
	
	private void processDirectedPresence(IProcessingContext context, Presence presence) {
		try {
			resourcesRegister.setDirectedPresence(context.getJid(), presence.getTo(), presence);
		} catch (Exception e) {
			throw new ProtocolException(new InternalServerError("Can't set resource's directed presence.", e));
		}
		
		// Server Rules for Handling XML Stanzas(rfc3920 11)
		if (presence.getTo().getResource() == null) {
			IResource[] resources = resourcesService.getResources(presence.getTo());
			for (IResource resource : resources) {
				if (resource.isAvailable()) {
					context.write(resource.getJid(), presence);
				}
			}
		} else {
			IResource resource = resourcesService.getResource(presence.getTo());
			if (resource.isAvailable()) {
				context.write(presence);
			}
		}
	}

	protected boolean isInstantMessagingUser(JabberId jid) {
		return authenticator.exists(jid.getNode());
	}

	@Override
	public void setEventFirer(IEventFirer eventFirer) {
		this.eventFirer = eventFirer;
	}

}
