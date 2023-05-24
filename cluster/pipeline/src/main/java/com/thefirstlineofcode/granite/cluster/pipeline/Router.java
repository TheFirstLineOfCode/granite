package com.thefirstlineofcode.granite.cluster.pipeline;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.IForward;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.IRouter;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.RoutingRegistrationException;
import com.thefirstlineofcode.granite.framework.core.repository.IInitializable;
import com.thefirstlineofcode.granite.framework.core.session.ISession;
import com.thefirstlineofcode.granite.framework.core.session.ISessionManager;

@Component("cluster.router")
public class Router implements IRouter, IInitializable {
	private static final String KEY_LOCAL_NODE_ID = "local.node.id";
	
	private static final Logger logger = LoggerFactory.getLogger(Router.class);
	
	@Dependency("session.manager")
	private ISessionManager sessionManager;
	
	@Dependency("ignite")
	private Ignite ignite;
	
	private ConcurrentMap<String, String> deliveryMessageQueueNames;
	
	private IgniteMessaging messaging;

	@Override
	public void register(JabberId jid, String localNodeId) throws RoutingRegistrationException {
		ISession session = sessionManager.get(jid);
		if (session == null)
			throw new RoutingRegistrationException("Null session.");
		
		session.setAttribute(KEY_LOCAL_NODE_ID, localNodeId);
		sessionManager.put(jid, session);
	}

	@Override
	public void unregister(JabberId jid) throws RoutingRegistrationException {
		ISession session = sessionManager.get(jid);
		if (session == null)
			throw new RoutingRegistrationException("Null session.");
		
		session.removeAttribute(KEY_LOCAL_NODE_ID);
	}

	@Override
	public IForward[] get(JabberId jid) {
		if (!jid.isBareId()) {
			ISession session = sessionManager.get(jid);
			if (session == null) {
				return new IForward[0];
			}
			
			String localNodeId = session.getAttribute(KEY_LOCAL_NODE_ID);
			if (localNodeId == null) {
				logger.warn("Null local node id. Session jid: {}.", session.getJid());
				return new IForward[0];
			}
			
			return new IForward[] {new Forward(localNodeId)};
		} else {
			throw new UnsupportedOperationException("Delivery message to a bare JID not supported yet.");
		}
	}
	
	private class Forward implements IForward {
		private String localNodeId;
		
		public Forward(String localNodeId) {
			this.localNodeId = localNodeId;
		}

		@Override
		public void to(Object message) {
			String deliveryMessageQueueName = deliveryMessageQueueNames.get(localNodeId);
			if (deliveryMessageQueueName == null) {
				deliveryMessageQueueName = "delivery-message-queue-" + localNodeId;
				deliveryMessageQueueNames.putIfAbsent(localNodeId, deliveryMessageQueueName);
			}
			
			messaging.send(deliveryMessageQueueName, message);
			
			if (logger.isTraceEnabled())
				logger.trace("Message {} has forwad to local node '{}'.", message, localNodeId);
		}
		
	}

	@Override
	public void init() {
		deliveryMessageQueueNames = new ConcurrentHashMap<>();
		messaging = ignite.message();
	}

}
