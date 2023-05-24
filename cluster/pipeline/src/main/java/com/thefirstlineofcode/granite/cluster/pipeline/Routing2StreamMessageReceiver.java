package com.thefirstlineofcode.granite.cluster.pipeline;

import java.util.UUID;

import org.apache.ignite.Ignite;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionManager;
import com.thefirstlineofcode.granite.framework.core.pipeline.AbstractMessageReceiver;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.ILocalNodeIdProvider;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.stream.IDeliveryMessageReceiver;

@Component("cluster.routing.2.stream.message.receiver")
public class Routing2StreamMessageReceiver extends AbstractMessageReceiver implements IDeliveryMessageReceiver {
	private static final Logger logger = LoggerFactory.getLogger(Routing2StreamMessageReceiver.class);
	
	@Dependency("ignite")
	private Ignite ignite;
	
	@Dependency("local.node.id.provider")
	private ILocalNodeIdProvider localNodeIdProvider;
	
	private IgniteBiPredicate<UUID, IMessage> messagePredicate;
	
	private IConnectionManager connectionManager;
	
	@Override
	public IConnectionContext getConnectionContext(JabberId sessionJid) {
		return connectionManager.getConnectionContext(sessionJid);
	}

	@Override
	protected void doStart() throws Exception {
		messagePredicate = new MessageBiPredicate();
		ignite.message().localListen("delivery-message-queue-" + localNodeIdProvider.getLocalNodeId(),
				messagePredicate);
	}
	
	private class MessageBiPredicate implements IgniteBiPredicate<UUID, IMessage> {
		private static final long serialVersionUID = 7455484644107041064L;

		@Override
		public boolean apply(UUID nodeId, IMessage message) {
			JabberId jid = (JabberId)(message.getHeaders().get(IMessage.KEY_MESSAGE_TARGET));
			
			if (jid == null) {
				logger.warn("Null message target. Message: {}.", message.getPayload());
				return true;
			}
			
			IConnectionContext context = getConnectionContext(jid);
			if (context != null) {
				messageProcessor.process(context, message);
			} else {
				// TODO process offline message
			}
			
			return true;
		}
	}

	@Override
	protected void doStop() throws Exception {
		ignite.message().stopLocalListen("delivery-message-queue-" + localNodeIdProvider.getLocalNodeId(), messagePredicate);
	}

	@Override
	public void setConnectionManager(IConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

}
