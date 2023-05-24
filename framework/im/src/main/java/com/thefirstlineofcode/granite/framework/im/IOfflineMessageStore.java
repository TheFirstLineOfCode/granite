package com.thefirstlineofcode.granite.framework.im;

import java.util.Iterator;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;

public interface IOfflineMessageStore {
	void save(JabberId jid, String messageId, String message);
	OfflineMessage get(JabberId jid, String messageId);
	void remove(JabberId jid, String messageId);
	Iterator<OfflineMessage> iterator(JabberId jid);
	boolean isEmpty(JabberId jid);
	int getSize(JabberId jid);
}
