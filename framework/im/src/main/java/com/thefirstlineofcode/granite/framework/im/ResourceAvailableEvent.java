package com.thefirstlineofcode.granite.framework.im;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEvent;

public class ResourceAvailableEvent implements IEvent {
	private JabberId jid;
	
	public ResourceAvailableEvent(JabberId jid) {
		this.jid = jid;
	}
	
	public JabberId getJid() {
		return jid;
	}
	
	@Override
	public Object clone() {
		return new ResourceAvailableEvent(jid);
	}
	
	@Override
	public String toString() {
		return String.format("ResourceAvailableEvent[JID=%s]", jid);
	}
}
