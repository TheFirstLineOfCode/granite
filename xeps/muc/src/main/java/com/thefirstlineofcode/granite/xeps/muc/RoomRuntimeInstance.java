package com.thefirstlineofcode.granite.xeps.muc;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import com.thefirstlineofcode.basalt.xeps.muc.Role;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.Conflict;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;

public class RoomRuntimeInstance implements IRoomRuntimeInstance {
	private OccupantsManager occupantsManager;
	private volatile Message subject;
	private BlockingQueue<Message> discussionHistory;
	
	public RoomRuntimeInstance(int maxHistory) {
		occupantsManager = new OccupantsManager();
		discussionHistory = new ArrayBlockingQueue<>(maxHistory);
	}

	@Override
	public void setSubject(Message subject) {
		this.subject = subject;
	}

	@Override
	public Message getSubject() {
		return subject;
	}

	@Override
	public synchronized void addToDiscussionHistory(Message message) {
		if (discussionHistory.remainingCapacity() == 0) {
			discussionHistory.poll();
		}
		
		discussionHistory.offer(message);
	}

	@Override
	public Occupant[] getOccupants() {
		return occupantsManager.getOccupants();
	}

	@Override
	public Occupant getOccupant(String nick) {
		return occupantsManager.getOccupant(nick);
	}

	@Override
	public void enter(JabberId sessionJid, String nick, Role role) {
		occupantsManager.enter(sessionJid, nick, role);
	}

	@Override
	public void exit(JabberId sessionJid) {
		occupantsManager.exit(sessionJid);
	}
	
	private class OccupantsManager {
		private Map<String, Occupant> occupants = new ConcurrentHashMap<>();
		
		public Occupant[] getOccupants() {
			Collection<Occupant> cOccupants = occupants.values();
			return cOccupants.toArray(new Occupant[cOccupants.size()]);
		}
		
		public Occupant getOccupant(String nick) {
			return occupants.get(nick);
		}
		
		public synchronized void enter(JabberId sessionJid, String nick, Role role) {
			Occupant occupant = occupants.get(nick);
			if (occupant == null) {
				occupant = createOccupant(sessionJid, nick, role);;
				occupants.put(nick, occupant);
			} else {
				// (xep-0045 7.2.9)
				// nickname conflict
				if (!occupant.getJids()[0].getBareId().equals(sessionJid.getBareId())) {
					throw new ProtocolException(new Conflict());
				}
				
				for (JabberId occupantJid : occupant.getJids()) {
					if (occupantJid.equals(sessionJid)) {
						return;
					}
				}
				
				occupant.addJid(sessionJid);
			}
		}

		public synchronized void exit(JabberId sessionJid) {
			Occupant occupant = findOccupant(sessionJid);
			
			if (occupant != null) {
				occupant.removeJid(sessionJid);
				
				if (occupant.getJids().length == 0) {
					occupants.remove(occupant.getNick());
				}
			}
		}

		private Occupant findOccupant(JabberId sessionJid) {
			Occupant occupant = null;
			top_loop:
			for (Entry<String, Occupant> entry : occupants.entrySet()) {
				for (JabberId aSessionJid : entry.getValue().getJids()) {
					if (aSessionJid.equals(sessionJid)) {
						occupant = entry.getValue();
						break top_loop;
					}
				}
			}
			
			return occupant;
		}
		
		private Occupant createOccupant(JabberId sessionJid, String nick, Role role) {
			Occupant occupant = new Occupant();
			occupant.setNick(nick);
			occupant.setRole(role);
			occupant.addJid(sessionJid);
			
			return occupant;
		}

		public synchronized void changeNick(JabberId sessionJid, String nick) {
			Occupant oldOccupant = findOccupant(sessionJid);
			
			if (oldOccupant == null) {
				throw new RuntimeException(String.format("Occupant(session JID: %s) not found.", sessionJid));
			}
			
			// (xep-0045 7.2.9)
			// nickname conflict
			Occupant sameNickOccupant = occupants.get(nick);
			if (sameNickOccupant != null && !sameNickOccupant.getJids()[0].getBareId().equals(sessionJid.getBareId())) {
				throw new ProtocolException(new Conflict());
			}
			
			oldOccupant.removeJid(sessionJid);
			if (oldOccupant.getJids().length == 0) {
				occupants.remove(oldOccupant.getNick());
			}
			
			if (sameNickOccupant != null) {
				sameNickOccupant.addJid(sessionJid);
			} else {
				Occupant newOccupant = new Occupant();
				newOccupant.setRole(oldOccupant.getRole());
				newOccupant.setNick(nick);
				newOccupant.addJid(sessionJid);
				occupants.put(nick, newOccupant);
			}
		}
	}

	@Override
	public Message[] getDiscussionHistory() {
		Object[] oMessages = discussionHistory.toArray();
		
		Message[] messages = new Message[oMessages.length];
		for (int i = 0; i < oMessages.length; i++) {
			messages[i] = (Message)oMessages[i];
		}
		
		return messages;
	}

	@Override
	public void changeNick(JabberId sessionJid, String nick) {
		occupantsManager.changeNick(sessionJid, nick);
	}

}
