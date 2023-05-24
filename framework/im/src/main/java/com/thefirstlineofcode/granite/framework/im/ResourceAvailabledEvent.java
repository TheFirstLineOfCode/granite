package com.thefirstlineofcode.granite.framework.im;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEvent;

public class ResourceAvailabledEvent implements IEvent {
	private JabberId jid;
	
	public ResourceAvailabledEvent(JabberId jid) {
		this.jid = jid;
	}
	
	public JabberId getJid() {
		return jid;
	}
	
	@Override
	public Object clone() {
		return new ResourceAvailabledEvent(jid);
	}
	
	@Override
	public String toString() {
		return String.format("ResourceAvailabledEvent[JID=%s]", jid);
	}
}
