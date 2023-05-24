package com.thefirstlineofcode.granite.xeps.muc;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.granite.framework.core.annotations.BeanDependency;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.framework.im.IPresenceProcessor;

public class GroupChatPresenceProcessor implements IPresenceProcessor {
	@Dependency("muc.protocols.delegator")
	private MucProtocolsDelegator delegator;
	
	@BeanDependency
	private IRoomService roomService;
	
	@Override
	public boolean process(IProcessingContext context, Presence presence) {
		if (presence.getType() != null && presence.getType() != Presence.Type.UNAVAILABLE)
			return false;
		
		if (presence.getTo() == null) {
			return false;
		}
		
		JabberId roomJid = presence.getTo().getBareId();
		if (!roomService.exists(roomJid)) {
			return false;
		}
		
		delegator.process(context, presence);
		
		return true;
	}

}

