package com.thefirstlineofcode.granite.cluster.pipeline;

import java.util.HashMap;
import java.util.Map;

import com.thefirstlineofcode.basalt.xmpp.core.IError;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Stream;
import com.thefirstlineofcode.granite.cluster.node.commons.deploying.NodeType;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageChannel;
import com.thefirstlineofcode.granite.framework.core.pipeline.SimpleMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.framework.core.repository.IInitializable;
import com.thefirstlineofcode.granite.framework.core.session.ISession;

@Component("cluster.parsing.2.processing.message.receiver")
public class Parsing2ProcessingMessageReceiver extends LocalMessageConnector implements IInitializable {
	private static final String CONFIGURATION_KEY_PARSING_2_PROCESSING_MESSAGE_QUEUE_MAX_SIZE = "parsing.2.processing.message.queue.max.size";
	private static final int DEFAULT_MESSAGE_QUEUE_MAX_SIZE = 1024 * 64;
	
	@Dependency(Constants.DEPENDENCY_ID_NODE_RUNTIME_CONFIGURATION)
	private NodeRuntimeConfiguration nodeRuntimeConfiguration;
	
	@Override
	protected int getDefaultMessageQueueMaxSize() {
		return DEFAULT_MESSAGE_QUEUE_MAX_SIZE;
	}

	@Override
	protected String getMessageQueueMaxSizeConfigurationKey() {
		return CONFIGURATION_KEY_PARSING_2_PROCESSING_MESSAGE_QUEUE_MAX_SIZE;
	}
	
	@Override
	protected IConnectionContext doGetConnectionContext(ISession session) {
		return new ProcessingConnectionContext(messageChannel, session);
	}
	
	private class ProcessingConnectionContext extends AbstractConnectionContext implements IProcessingContext {
		
		public ProcessingConnectionContext(IMessageChannel messageChannel, ISession session) {
			super(messageChannel, session);
		}

		@Override
		public void close() {
			Map<Object, Object> headers = new HashMap<>();
			headers.put(IMessage.KEY_SESSION_JID, session.getJid());
			headers.put(IMessage.KEY_MESSAGE_TARGET, session.getJid());
			
			IMessage message = new SimpleMessage(headers, new Stream(true));
			
			messageChannel.send(new SimpleMessage(headers, message));
		}

		@Override
		protected boolean isMessageAccepted(Object message) {
			Class<?> messageType = message.getClass();
			return (Stanza.class.isAssignableFrom(messageType)) ||
					(IError.class.isAssignableFrom(messageType)) ||
					(Stream.class == messageType) ||
					(IMessage.class.isAssignableFrom(messageType));
		}
		
		private IMessage createMessage(JabberId target, Object message) {
			Map<Object, Object> headers = new HashMap<>();
			headers.put(IMessage.KEY_SESSION_JID, session.getJid());
			headers.put(IMessage.KEY_MESSAGE_TARGET, target);
			
			return new SimpleMessage(headers, message);
		}

		@Override
		public void write(JabberId target, Object message) {
			messageChannel.send(createMessage(target, message));
		}
		
	}

	@Override
	public void init() {
		NodeType nodeType = nodeRuntimeConfiguration.getDeployPlan().getNodeTypes().get(nodeRuntimeConfiguration.getNodeType());
		if (!nodeType.hasAbility("stream")) {
			throw new RuntimeException("Splitting application vertically isn't supported yet. Appnode must possess all IM abilities(stream, processing, event).");
		}
	}

}
