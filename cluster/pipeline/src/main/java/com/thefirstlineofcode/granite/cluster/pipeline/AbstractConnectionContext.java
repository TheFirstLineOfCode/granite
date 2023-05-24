package com.thefirstlineofcode.granite.cluster.pipeline;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageChannel;
import com.thefirstlineofcode.granite.framework.core.pipeline.SimpleMessage;
import com.thefirstlineofcode.granite.framework.core.session.ISession;
import com.thefirstlineofcode.granite.framework.core.session.ValueWrapper;

public abstract class AbstractConnectionContext implements IConnectionContext {
	private static final Logger logger = LoggerFactory.getLogger(AbstractConnectionContext.class);
	
	protected IMessageChannel messageChannel;
	protected ISession session;
	
	public AbstractConnectionContext(IMessageChannel messageChannel, ISession session) {
		this.messageChannel = messageChannel;
		this.session = session;
	}

	@Override
	public <T> T setAttribute(Object key, T value) {
		return session.setAttribute(key, value);
	}

	@Override
	public <T> T getAttribute(Object key) {
		return session.getAttribute(key);
	}

	@Override
	public <T> T getAttribute(Object key, T defaultValue) {
		return session.getAttribute(key, defaultValue);
	}

	@Override
	public <T> T removeAttribute(Object key) {
		return session.removeAttribute(key);
	}
	
	@Override
	public <T> T setAttribute(Object key, ValueWrapper<T> wrapper) {
		return session.setAttribute(key, wrapper);
	}

	@Override
	public Object[] getAttributeKeys() {
		return session.getAttributeKeys();
	}

	@Override
	public JabberId getJid() {
		return session.getJid();
	}
	
	@Override
	public void write(Object message) {
		if (isMessageAccepted(message)) {
			messageChannel.send(createMessage(message));
		} else {
			logger.warn("Ignore an unaccepted message. Connection context: {}. Message type: {}.",
					getClass().getName(), message.getClass().getName());
		}
	}
	
	protected abstract boolean isMessageAccepted(Object message);

	protected IMessage createMessage(Object message) {
		if (IMessage.class.isAssignableFrom(message.getClass()))
			return (IMessage)message;
		
		Map<Object, Object> headers = new HashMap<>();
		headers.put(IMessage.KEY_SESSION_JID, session.getJid());
		
		return new SimpleMessage(headers, message);
	}
	
}

