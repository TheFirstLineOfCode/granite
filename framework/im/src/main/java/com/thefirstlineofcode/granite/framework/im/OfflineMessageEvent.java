package com.thefirstlineofcode.granite.framework.im;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEvent;

public class OfflineMessageEvent implements IEvent {
	private JabberId user;
	private JabberId contact;
	private Message message;
	
	public OfflineMessageEvent(JabberId user, JabberId contact, Message message) {
		this.user = user;
		this.contact = contact;
		this.message = message;
	}

	public JabberId getUser() {
		return user;
	}

	public void setUser(JabberId user) {
		this.user = user;
	}

	public JabberId getContact() {
		return contact;
	}

	public void setContact(JabberId contact) {
		this.contact = contact;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}
	
	@Override
	public Object clone() {
		return new OfflineMessageEvent(user, contact, message);
	}
	
	@Override
	public String toString() {
		return String.format("OfflineMessageEvent[User=%s, Contact=%s, Message=%s]",
				user, contact, message.toString());
	}
	
}
