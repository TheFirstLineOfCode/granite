package com.thefirstlineofcode.granite.cluster.im;

import java.util.HashMap;
import java.util.Map;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.granite.framework.im.IResource;

public class Resource implements IResource {
	private JabberId jid;
	private boolean rosterRequested;
	private Presence broadcastPresence;
	private boolean available;
	private Map<JabberId, Presence> directedPresences;
	
	public Resource(JabberId jid) {
		this.jid = jid;
		rosterRequested = false;
		directedPresences = new HashMap<>();
	}

	@Override
	public JabberId getJid() {
		return jid;
	}

	@Override
	public boolean isRosterRequested() {
		return rosterRequested;
	}

	@Override
	public Presence getBroadcastPresence() {
		return broadcastPresence;
	}
	
	@Override
	public boolean isAvailable() {
		return available;
	}

	@Override
	public Presence getDirectedPresence(JabberId from) {
		return directedPresences.get(from);
	}
	
	public void setDirectedPresence(JabberId from, Presence presence) {
		directedPresences.put(from, presence);
	}

	public void setRosterRequested(boolean rosterRequested) {
		this.rosterRequested = rosterRequested;
	}

	public void setBroadcastPresence(Presence broadcastPresence) {
		this.broadcastPresence = broadcastPresence;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

}
