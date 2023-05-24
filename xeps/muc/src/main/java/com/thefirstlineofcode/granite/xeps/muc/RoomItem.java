package com.thefirstlineofcode.granite.xeps.muc;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;

public class RoomItem {
	private JabberId jid;
	private String name;
	
	public JabberId getJid() {
		return jid;
	}
	
	public void setJid(JabberId jid) {
		this.jid = jid;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
}
