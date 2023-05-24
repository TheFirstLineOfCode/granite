package com.thefirstlineofcode.granite.framework.core.pipeline.stages.event;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;

public class SessionEstablishedEvent implements IEvent {
	private String connectionId;
	private JabberId jid;
	
	public SessionEstablishedEvent(String connectionId, JabberId jid) {
		this.connectionId = connectionId;
		this.jid = jid;
	}
	
	public String getConnectionId() {
		return connectionId;
	}
	
	public JabberId getJid() {
		return jid;
	}
	
	@Override
	public Object clone() {
		return new SessionEstablishedEvent(connectionId, jid);
	}
	
	@Override
	public String toString() {
		return String.format("SessionEstablishedEvent[Connection ID=%s, JID=%s]",
				connectionId, jid);
	}
}
