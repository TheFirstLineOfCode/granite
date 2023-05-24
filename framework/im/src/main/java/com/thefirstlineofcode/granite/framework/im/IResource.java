package com.thefirstlineofcode.granite.framework.im;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;

public interface IResource {
	JabberId getJid();
	boolean isRosterRequested();
	Presence getBroadcastPresence();
	boolean isAvailable();
	Presence getDirectedPresence(JabberId from);
}
