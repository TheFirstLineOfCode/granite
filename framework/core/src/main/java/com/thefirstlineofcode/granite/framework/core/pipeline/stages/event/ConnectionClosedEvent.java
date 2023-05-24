package com.thefirstlineofcode.granite.framework.core.pipeline.stages.event;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;

public class ConnectionClosedEvent implements IEvent {
	private String connectionId;
	private JabberId jid;
	private String streamId;
	
	public ConnectionClosedEvent(String connectionId, JabberId jid, String streamId) {
		this.connectionId = connectionId;
		this.jid = jid;
		this.streamId = streamId;
	}
	
	public String getConnectionId() {
		return connectionId;
	}
	
	public JabberId getJid() {
		return jid;
	}

	public String getStreamId() {
		return streamId;
	}
	
	@Override
	public Object clone() {
		return new ConnectionClosedEvent(connectionId, jid, streamId);
	}
	
	@Override
	public String toString() {
		return String.format("ConnectionClosedEvent[Stream ID=%s, Connection ID=%s, JID=%s]",
				streamId, connectionId, jid);
	}
	
}
