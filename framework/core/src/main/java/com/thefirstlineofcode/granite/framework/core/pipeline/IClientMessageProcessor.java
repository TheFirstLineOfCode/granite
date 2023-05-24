package com.thefirstlineofcode.granite.framework.core.pipeline;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.connection.IClientConnectionContext;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionManager;

public interface IClientMessageProcessor extends IMessageProcessor {
	void setConnectionManager(IConnectionManager connectionManager);
	void connectionOpened(IClientConnectionContext context);
	void connectionClosing(IClientConnectionContext context);
	void connectionClosed(IClientConnectionContext context, JabberId sessionJid);
}
