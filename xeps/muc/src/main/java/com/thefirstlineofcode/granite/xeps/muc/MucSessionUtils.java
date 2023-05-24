package com.thefirstlineofcode.granite.xeps.muc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.session.ISession;

public abstract class MucSessionUtils {
	private MucSessionUtils() {}
	
	public static final String SESSION_KEY_MUC_ROOMJID_AND_NICK_MAP = "granite.session.key.muc.roomjid.and.nick.map";
	
	public static Map<JabberId, String> getOrCreateRoomJidToNicks(ISession session) {
		Map<JabberId, String> roomJidToNicks = session.getAttribute(SESSION_KEY_MUC_ROOMJID_AND_NICK_MAP);
		if (roomJidToNicks == null) {
			roomJidToNicks = new ConcurrentHashMap<>();
			Map<JabberId, String> previous = session.setAttribute(SESSION_KEY_MUC_ROOMJID_AND_NICK_MAP,
					roomJidToNicks);
			if (previous != null) {
				roomJidToNicks = previous;
			}
		}
		
		return roomJidToNicks;
	}
}
