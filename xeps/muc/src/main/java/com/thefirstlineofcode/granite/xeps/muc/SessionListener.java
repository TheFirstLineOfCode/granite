package com.thefirstlineofcode.granite.xeps.muc;

import java.util.Map;
import java.util.Map.Entry;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.framework.core.session.ISessionListener;

public class SessionListener implements ISessionListener {
	@Dependency("muc.protocols.delegator")
	private MucProtocolsDelegator mucProtocolsDelegator;
	
	@Override
	public void sessionEstablished(IConnectionContext context, JabberId sessionJid) {}

	@Override
	public void sessionClosing(IConnectionContext context, JabberId sessionJid) {
		Map<JabberId, String> roomJidToNicks = MucSessionUtils.getOrCreateRoomJidToNicks(context);
		for (Entry<JabberId, String> roomJidAndNick : roomJidToNicks.entrySet()) {
			mucProtocolsDelegator.exitRoom((IProcessingContext)context, roomJidAndNick.getKey(), roomJidAndNick.getValue());
		}
	}

	@Override
	public void sessionClosed(IConnectionContext context, JabberId sessionJid) {}

	@Override
	public void sessionEstablishing(IConnectionContext context, JabberId sessionJid) {}

}
