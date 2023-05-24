package com.thefirstlineofcode.granite.xeps.muc;

import com.thefirstlineofcode.basalt.xeps.address.Address;
import com.thefirstlineofcode.basalt.xeps.address.Addresses;
import com.thefirstlineofcode.basalt.xeps.delay.Delay;
import com.thefirstlineofcode.basalt.xeps.muc.Affiliation;
import com.thefirstlineofcode.basalt.xeps.muc.Role;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.Forbidden;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.NotAuthorized;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.RegistrationRequired;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.ServiceUnavailable;
import com.thefirstlineofcode.basalt.xmpp.datetime.DateTime;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.granite.framework.core.utils.StanzaCloner;

public class RoomSession implements IRoomSession {
	private Room room;
	private IRoomRuntimeInstance runtimeInstance;
	
	public RoomSession(Room room, IRoomRuntimeInstance runtimeInstance) {
		this.room = room;
		this.runtimeInstance = runtimeInstance;
	}

	@Override
	public Occupant[] getOccupants() {
		return runtimeInstance.getOccupants();
	}

	@Override
	public Occupant getOccupant(String nick) {
		return runtimeInstance.getOccupant(nick);
	}

	@Override
	public void enter(JabberId sessionJid, String nick, String secret) {
		if (room.getRoomConfig().isPasswordProtectedRoom()) {
			// (xep-0045 7.2.6)
			// password-protected rooms
			if (secret == null || !secret.equals(room.getRoomConfig().getRoomSecret())) {
				throw new ProtocolException(new NotAuthorized());
			}
		}
		
		if (getOccupants().length >= room.getRoomConfig().getMaxUsers()) {
			// (xep-0045 7.2.10)
			// max users
			throw new ProtocolException(new ServiceUnavailable());
		}
		
		runtimeInstance.enter(sessionJid, nick, getOccupantRole(sessionJid));
	}
	
	// (xep-0045 5.1.2)
	// default roles
	private Role getOccupantRole(JabberId sessionJid) {
		AffiliatedUser affiliatedUser = room.getAffiliatedUser(sessionJid.getBareId());
		if (affiliatedUser == null) {
			if (room.getRoomConfig().isMembersOnly()) {
				// (xep-0045 7.2.7)
				// members-only rooms
				throw new ProtocolException(new RegistrationRequired());
			}
			
			if (room.getRoomConfig().isModeratedRoom()) {
				return Role.VISITOR;
			}
			
			return Role.PARTICIPANT;
		}
		
		if (affiliatedUser.getRole() != null)
			return affiliatedUser.getRole();
		
		if (affiliatedUser.getAffiliation() == Affiliation.MEMBER) {
			return Role.PARTICIPANT;
		} else if (affiliatedUser.getAffiliation() == Affiliation.ADMIN ||
				affiliatedUser.getAffiliation() == Affiliation.OWNER) {
			return Role.MODERATOR;
		} else {
			// affiliatedUser.getAffiliation() == Affiliation.OUTCAST
			// (xep-0045 7.2.8)
			// banned users
			throw new ProtocolException(new Forbidden());
		}
	}

	@Override
	public void exit(JabberId sessionJid) {
		runtimeInstance.exit(sessionJid);
	}

	@Override
	public void enter(JabberId sessionJid, String nick) {
		enter(sessionJid, nick, null);
	}

	@Override
	public Room getRoom() {
		return room;
	}

	@Override
	public void setSubject(Message subject) {
		runtimeInstance.setSubject(subject);
	}

	@Override
	public Message getSubject() {
		return runtimeInstance.getSubject();
	}

	@Override
	public void addToDiscussionHistory(String nick, JabberId oFrom, Message message) {
		Message history = StanzaCloner.clone(message);
		
		JabberId roomJid = room.getRoomJid();
		
		Delay delay = new Delay();
		delay.setFrom(roomJid);
		delay.setStamp(new DateTime());
		
		history.getObjects().add(delay);
		history.getObjectProtocols().put(Delay.class, Delay.PROTOCOL);
		
		Address address = new Address();
		address.setJid(oFrom);
		
		Addresses addresses = new Addresses();
		addresses.getAddresses().add(address);
		
		history.getObjects().add(addresses);
		history.getObjectProtocols().put(Addresses.class, Addresses.PROTOCOL);
		
		history.setFrom(new JabberId(roomJid.getNode(), roomJid.getDomain(), nick));
		
		runtimeInstance.addToDiscussionHistory(history);
	}

	@Override
	public Message[] getDiscussionHistory() {
		return runtimeInstance.getDiscussionHistory();
	}

	@Override
	public void changeNick(JabberId sessionJid, String nick) {
		runtimeInstance.changeNick(sessionJid, nick);
	}

}
