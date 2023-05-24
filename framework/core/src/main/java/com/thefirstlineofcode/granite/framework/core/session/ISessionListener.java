package com.thefirstlineofcode.granite.framework.core.session;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.IPipelineExtender;

public interface ISessionListener extends IPipelineExtender {
	void sessionEstablishing(IConnectionContext context, JabberId sessionJid) throws Exception;
	void sessionEstablished(IConnectionContext context, JabberId sessionJid) throws Exception;
	void sessionClosing(IConnectionContext context, JabberId sessionJid) throws Exception;
	void sessionClosed(IConnectionContext context, JabberId sessionJid) throws Exception;
}
