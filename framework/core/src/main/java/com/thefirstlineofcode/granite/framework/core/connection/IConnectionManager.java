package com.thefirstlineofcode.granite.framework.core.connection;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;

public interface IConnectionManager {
	IConnectionContext getConnectionContext(JabberId sessionJid);
}
