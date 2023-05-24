package com.thefirstlineofcode.granite.lite.session;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.config.IConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.session.ISession;
import com.thefirstlineofcode.granite.framework.core.session.ISessionCallback;
import com.thefirstlineofcode.granite.framework.core.session.ISessionManager;
import com.thefirstlineofcode.granite.framework.core.session.SessionExistsException;
import com.thefirstlineofcode.granite.framework.core.session.ValueWrapper;

@Component("lite.session.manager")
public class SessionManager implements ISessionManager, IConfigurationAware {
	private static final String KEY_SESSION_CALLBACK_CHECK_INTERVAL = "session.callback.check.interval";
	private static final int DEFAULT_SESSION_CALLBACK_CHECK_INTERVAL = 5 * 1000;
	
	private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
	
	private ConcurrentMap<JabberId, ISession> sessions = new ConcurrentHashMap<>();
	
	private Thread callbackThread;
	
	private int sessionCallbackCheckInterval;
	
	public SessionManager() {
		callbackThread = new Thread(new CallbackRunnable(), "Granite Session Callback Thread");
		callbackThread.start();
	}
	
	private class CallbackRunnable implements Runnable {

		@Override
		public void run() {
			try {
				for (Entry<JabberId, ISession> sessionEntry : sessions.entrySet()) {
					checkAndInvokeCallback(sessionEntry.getKey(), sessionEntry.getValue());
				}
				
				Thread.sleep(sessionCallbackCheckInterval);
			} catch (InterruptedException e) {
				logger.error("Session callback thread interrupted. Session manager will try to restart it...", e);
				
				callbackThread = new Thread(new CallbackRunnable());
				callbackThread.start();
			} catch (Exception e) {
				logger.warn("Something wrong while checking and invoking session callback.", e);
			}
		}

		private void checkAndInvokeCallback(JabberId jid, ISession session) {
			for (Object key : session.getAttributeKeys()) {
				Object value = session.getAttribute(key);
				
				if (value != null && value instanceof ISessionCallback) {
					((ISessionCallback)value).invoke(session);
				}
			}
		}
		
	}
	
	@Override
	public ISession create(JabberId jid) throws SessionExistsException {
		ISession session = new Session(jid);
		ISession previous = sessions.putIfAbsent(jid, session);
		
		if (previous != null)
			throw new SessionExistsException(String.format("Session '%s' has existed.", jid));
		
		return session;
	}

	@Override
	public ISession get(JabberId jid) {
		return sessions.get(jid);
	}

	@Override
	public boolean exists(JabberId jid) {
		return sessions.containsKey(jid);
	}

	@Override
	public boolean remove(JabberId jid) {
		return sessions.remove(jid) != null;
	}
	
	private class Session implements ISession {
		private JabberId id;
		private ConcurrentMap<Object, Object> attributes;
		
		public Session(JabberId id) {
			this.id = id;
			attributes = new ConcurrentHashMap<>();
		}

		@Override
		public <T> T setAttribute(Object key, T value) {
			Object previous = attributes.putIfAbsent(key, value);
			if (previous != null)
				return unwrapIfNeed(previous);
			
			return value;
		}

		@Override
		public <T> T getAttribute(Object key) {
			return unwrapIfNeed(attributes.get(key));
		}

		@Override
		public <T> T getAttribute(Object key, T defaultValue) {
			T value = getAttribute(key);
			
			return value != null ? value : defaultValue;
		}

		@Override
		public <T> T removeAttribute(Object key) {
			return unwrapIfNeed(attributes.remove(key));
		}

		@SuppressWarnings("unchecked")
		private <T> T unwrapIfNeed(Object value) {
			if (value == null)
				return null;
			
			if (value instanceof ValueWrapper) {
				ValueWrapper<T> valueWrapper = (ValueWrapper<T>)value;
				return (T)valueWrapper.getValue();
			} else {
				return (T)value;
			}
		}

		@Override
		public Object[] getAttributeKeys() {
			return attributes.keySet().toArray();
		}

		@Override
		public JabberId getJid() {
			return id;
		}
		
		@Override
		public <T> T setAttribute(Object key, ValueWrapper<T> wrapper) {
			Object previous = attributes.putIfAbsent(key, wrapper);
			if (previous != null) {
				return unwrapIfNeed(previous);
			}
			
			return wrapper.getValue();
		}
		
	}

	@Override
	public void setConfiguration(IConfiguration configuration) {
		sessionCallbackCheckInterval = configuration.getInteger(KEY_SESSION_CALLBACK_CHECK_INTERVAL,
				DEFAULT_SESSION_CALLBACK_CHECK_INTERVAL);
	}

	@Override
	public void put(JabberId jid, ISession session) {
		sessions.put(jid, session);
	}

}
