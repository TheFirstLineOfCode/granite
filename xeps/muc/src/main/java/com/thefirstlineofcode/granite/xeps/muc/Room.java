package com.thefirstlineofcode.granite.xeps.muc;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.thefirstlineofcode.basalt.xeps.muc.RoomConfig;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;

public class Room {
	private JabberId roomJid;
	private JabberId creator;
	private Date createTime;
	private boolean locked;
	private RoomConfig roomConfig;
	private Map<JabberId, AffiliatedUser> affiliatedUsers;
	
	public Room() {
		affiliatedUsers = new HashMap<>();
	}

	public JabberId getRoomJid() {
		return roomJid;
	}
	
	public void setRoomJid(JabberId roomJid) {
		this.roomJid = roomJid;
	}
	
	public JabberId getCreator() {
		return creator;
	}
	
	public void setCreator(JabberId creator) {
		this.creator = creator;
	}
	
	public Date getCreateTime() {
		return createTime;
	}
	
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public RoomConfig getRoomConfig() {
		return roomConfig;
	}

	public void setRoomConfig(RoomConfig roomConfig) {
		this.roomConfig = roomConfig;
	}

	public AffiliatedUser[] getAffiliatedUsers() {
		Collection<AffiliatedUser> cAffiliatedUsers = affiliatedUsers.values();
		return cAffiliatedUsers.toArray(new AffiliatedUser[cAffiliatedUsers.size()]);
	}
	
	public AffiliatedUser getAffiliatedUser(JabberId jid) {
		if (jid.isBareId()) {
			return affiliatedUsers.get(jid);
		} else {
			return affiliatedUsers.get(jid.getBareId());
		}
		
	}
	
	public void addAffiliatedUser(AffiliatedUser affiliatedUser) {
		affiliatedUsers.put(affiliatedUser.getJid(), affiliatedUser);
	}
	
	public void removeAffiliatedUser(JabberId jid) {
		affiliatedUsers.remove(jid);
	}
	
}
