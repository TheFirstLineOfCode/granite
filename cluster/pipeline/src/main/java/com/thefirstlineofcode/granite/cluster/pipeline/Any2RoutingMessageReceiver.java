package com.thefirstlineofcode.granite.cluster.pipeline;

import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageChannel;
import com.thefirstlineofcode.granite.framework.core.session.ISession;

@Component("cluster.any.2.routing.message.receiver")
public class Any2RoutingMessageReceiver extends LocalMessageConnector {
	private static final String CONFIGURATION_KEY_ANY_2_ROUTING_MESSAGE_QUEUE_MAX_SIZE = "any.2.routing.message.queue.max.size";
	private static final int DEFAULT_MESSAGE_QUEUE_MAX_SIZE = 1024 * 64;
	
	@Override
	protected int getDefaultMessageQueueMaxSize() {
		return DEFAULT_MESSAGE_QUEUE_MAX_SIZE;
	}
	
	@Override
	protected String getMessageQueueMaxSizeConfigurationKey() {
		return CONFIGURATION_KEY_ANY_2_ROUTING_MESSAGE_QUEUE_MAX_SIZE;
	}

	@Override
	protected IConnectionContext doGetConnectionContext(ISession session) {
		return new RoutingConnectionContext(messageChannel, session);
	}
	
	private class RoutingConnectionContext extends AbstractConnectionContext {

		public RoutingConnectionContext(IMessageChannel messageChannel, ISession session) {
			super(messageChannel, session);
		}
		
		@Override
		public void close() {
			throw new UnsupportedOperationException("Can't call close operation in routing phase.");
		}
		
		@Override
		protected boolean isMessageAccepted(Object message) {
			Class<?> messageType = message.getClass();
			if (!IMessage.class.isAssignableFrom(messageType))
				return false;
			
			Object payload = ((IMessage)message).getPayload();
			
			return String.class == payload.getClass();
		}
	}
}
