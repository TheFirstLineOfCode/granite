package com.thefirstlineofcode.granite.lite.pipeline;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.oxm.OxmService;
import com.thefirstlineofcode.basalt.xmpp.core.IError;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Stream;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageChannel;
import com.thefirstlineofcode.granite.framework.core.pipeline.SimpleMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.framework.core.session.ISession;
import com.thefirstlineofcode.granite.framework.core.session.ValueWrapper;

public abstract class AbstractConnectionContext implements IConnectionContext {
	private static final Logger logger = LoggerFactory.getLogger(AbstractConnectionContext.class);
	
	protected IMessageChannel messageChannel;
	protected ISession session;
	
	public AbstractConnectionContext(ISession session, IMessageChannel messageChannel) {
		this.session = session;
		this.messageChannel = messageChannel;
	}

	@Override
	public <T> T setAttribute(Object key, T value) {
		if (session == null)
			return null;
		
		return session.setAttribute(key, value);
	}

	@Override
	public <T> T getAttribute(Object key) {
		if (session == null)
			return null;
		
		return session.getAttribute(key);
	}

	@Override
	public <T> T getAttribute(Object key, T defaultValue) {
		if (session == null)
			return defaultValue;
		
		return session.getAttribute(key, defaultValue);
	}

	@Override
	public <T> T removeAttribute(Object key) {
		if (session == null)
			return null;
		
		return session.removeAttribute(key);
	}
	
	@Override
	public <T> T setAttribute(Object key, ValueWrapper<T> wrapper) {
		if (session == null)
			return null;
		
		return session.setAttribute(key, wrapper);
	}

	@Override
	public Object[] getAttributeKeys() {
		if (session == null)
			return new Object[0];
		
		return session.getAttributeKeys();
	}

	@Override
	public JabberId getJid() {
		if (session == null)
			return null;
		
		return session.getJid();
	}
	
	@Override
	public void write(Object message) {
		if (isAcceptedType(message.getClass())) {
			messageChannel.send(createMessage(message));
		} else {
			logger.warn("Unaccepted type: {}.", message.getClass());
		}
	}
	
	protected IMessage createMessage(Object message) {
		Map<Object, Object> headers = new HashMap<>();
		headers.put(IMessage.KEY_SESSION_JID, session.getJid());
		
		return new SimpleMessage(headers, message);
	}
	
	@Override
	public void close() {
		Map<Object, Object> headers = new HashMap<>();
		headers.put(IMessage.KEY_SESSION_JID, session.getJid());
		
		messageChannel.send(new SimpleMessage(headers, getStreamCloseMessage()));
	}
	
	public static class MessageOutConnectionContext extends AbstractConnectionContext {
		public MessageOutConnectionContext(ISession session, IMessageChannel messageChannel) {
			super(session, messageChannel);
		}

		@Override
		protected Object getStreamCloseMessage() {
			Map<Object, Object> headers = new HashMap<>();
			headers.put(IMessage.KEY_SESSION_JID, session.getJid());
			headers.put(IMessage.KEY_MESSAGE_TARGET, session.getJid());
			
			return new SimpleMessage(headers, new Stream(true));
		}

		@Override
		protected boolean isAcceptedType(Class<?> type) {
			return (IMessage.class.isAssignableFrom(type));
		}
		
		@Override
		protected IMessage createMessage(Object message) {
			return (IMessage)message;
		}
	}

	public static class ProcessingContext extends ObjectOutConnectionContext implements IProcessingContext {

		public ProcessingContext(ISession session, IMessageChannel messageChannel) {
			super(session, messageChannel);
		}
		
		protected IMessage createMessage(JabberId target, Object message) {
			Map<Object, Object> headers = new HashMap<>();
			headers.put(IMessage.KEY_SESSION_JID, session.getJid());
			headers.put(IMessage.KEY_MESSAGE_TARGET, target);
			
			return new SimpleMessage(headers, message);
		}

		@Override
		public void write(JabberId target, Object message) {
			messageChannel.send(createMessage(target, message));
		}
		
		@Override
		protected boolean isAcceptedType(Class<?> type) {
			return (Stanza.class.isAssignableFrom(type)) ||
					(IError.class.isAssignableFrom(type)) ||
					(Stream.class == type) ||
					IMessage.class.isAssignableFrom(type);
		}
		
	}

	public static class StringOutConnectionContext extends AbstractConnectionContext {
		private static final String STREAM_CLOSE_MESSAGE = OxmService.createMinimumOxmFactory().translate(new Stream(true));

		public StringOutConnectionContext(ISession session, IMessageChannel messageChannel) {
			super(session, messageChannel);
		}

		@Override
		protected Object getStreamCloseMessage() {
			return STREAM_CLOSE_MESSAGE;
		}

		@Override
		protected boolean isAcceptedType(Class<?> type) {
			return type == String.class;
		}
		
	}

	public static class ObjectOutConnectionContext extends AbstractConnectionContext {
		protected static final Stream STREAM_CLOSE_MESSAGE = new Stream(true);

		public ObjectOutConnectionContext(ISession session, IMessageChannel messageChannel) {
			super(session, messageChannel);
		}

		@Override
		protected Object getStreamCloseMessage() {
			return STREAM_CLOSE_MESSAGE;
		}

		@Override
		protected boolean isAcceptedType(Class<?> type) {
			return (Stanza.class.isAssignableFrom(type)) ||
					(IError.class.isAssignableFrom(type)) ||
					(Stream.class == type);
		}
		
	}

	protected abstract Object getStreamCloseMessage();
	protected abstract boolean isAcceptedType(Class<?> type);
	
}

