package com.thefirstlineofcode.granite.cluster.pipeline;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageChannel;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEvent;
import com.thefirstlineofcode.granite.framework.core.session.ISession;

@Component("cluster.any.2.event.message.receiver")
public class Any2EventMessageReceiver extends LocalMessageConnector {
	private static final String CONFIGURATION_KEY_ANY_2_EVENT_MESSAGE_QUEUE_MAX_SIZE = "any.2.event.message.queue.max.size";
	private static final int DEFAULT_MESSAGE_QUEUE_MAX_SIZE = 1024 * 64;
	
	@Override
	protected int getDefaultMessageQueueMaxSize() {
		return DEFAULT_MESSAGE_QUEUE_MAX_SIZE;
	}
	
	@Override
	protected String getMessageQueueMaxSizeConfigurationKey() {
		return CONFIGURATION_KEY_ANY_2_EVENT_MESSAGE_QUEUE_MAX_SIZE;
	}
	
	@Override
	public IConnectionContext getConnectionContext(JabberId sessionJid) {
		return doGetConnectionContext(null);
	}
	
	@Override
	protected IConnectionContext doGetConnectionContext(ISession session) {
		return new EventConnectionContext(messageChannel);
	}
	
	private class EventConnectionContext extends AbstractConnectionContext {
		public EventConnectionContext(IMessageChannel messageChannel) {
			super(messageChannel, null);
		}
		
		@Override
		public void close() {
			throw new UnsupportedOperationException("Can't call close operation in event phase.");
		}
		
		@Override
		protected boolean isMessageAccepted(Object message) {
			Class<?> messageType = message.getClass();
			if (!IMessage.class.isAssignableFrom(messageType))
				return false;
			
			Object payload = ((IMessage)message).getPayload();
			
			return IEvent.class.isAssignableFrom(payload.getClass());
		}
	}
}
