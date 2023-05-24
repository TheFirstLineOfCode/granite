package com.thefirstlineofcode.granite.framework.core.session;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;

public interface ISessionManager {
	ISession create(JabberId jid) throws SessionExistsException;
	void put(JabberId jid, ISession session);
	ISession get(JabberId jid);
	boolean exists(JabberId jid);
	boolean remove(JabberId jid);
}
