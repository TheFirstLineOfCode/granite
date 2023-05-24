package com.thefirstlineofcode.granite.cluster.session;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.repository.IInitializable;
import com.thefirstlineofcode.granite.framework.core.session.ISession;
import com.thefirstlineofcode.granite.framework.core.session.ISessionManager;
import com.thefirstlineofcode.granite.framework.core.session.SessionExistsException;

@Component("cluster.session.manager")
public class SessionManager implements ISessionManager, IInitializable {
	
	@Dependency("ignite")
	private Ignite ignite;
	
	private SessionsStorageWrapper sessionsStorageWrapper;
	
	@Override
	public void init() {
		sessionsStorageWrapper = new SessionsStorageWrapper(ignite);
	}
	
	@Override
	public ISession create(JabberId jid) throws SessionExistsException {
		ISession session = new Session(jid);
		if (!getSessionsStorage().putIfAbsent(jid, session)) {
			throw new SessionExistsException();
		}
		
		return session;
	}

	@Override
	public ISession get(JabberId jid) {
		return getSessionsStorage().get(jid);
	}

	@Override
	public boolean exists(JabberId jid) {
		return getSessionsStorage().containsKey(jid);
	}

	@Override
	public boolean remove(JabberId jid) {
		return getSessionsStorage().remove(jid);
	}
	
	private class SessionsStorageWrapper {
		private Ignite ignite;
		private volatile IgniteCache<JabberId, ISession> sessions;
		
		public SessionsStorageWrapper(Ignite ignite) {
			this.ignite = ignite;
		}
		
		public IgniteCache<JabberId, ISession> getSessions() {
			if (sessions != null) {
				return sessions;
			}
			
			synchronized(SessionManager.this) {
				if (sessions != null)
					return sessions;
				
				sessions = ignite.cache("sessions");
			}
			
			return sessions;
		}
	}
	
	private IgniteCache<JabberId, ISession> getSessionsStorage() {
		return sessionsStorageWrapper.getSessions();
	}

	@Override
	public void put(JabberId jid, ISession session) {
		getSessionsStorage().put(jid, session);
	}
	
}
