package com.thefirstlineofcode.granite.xeps.muc;

import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.BadRequest;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.ItemNotFound;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.granite.framework.core.annotations.BeanDependency;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.framework.im.IMessageProcessor;

public class GroupChatMessageProcessor implements IMessageProcessor {
	@Dependency("muc.protocols.delegator")
	private MucProtocolsDelegator delegator;
	
	@BeanDependency
	private IRoomService roomService;

	@Override
	public boolean process(IProcessingContext context, Message message) {
		if (message.getType() == Message.Type.GROUPCHAT) {
			if (message.getTo() == null) {
				throw new ProtocolException(new BadRequest("Null room jid."));
			}
			
			if (!message.getTo().isBareId()) {
				throw new ProtocolException(new BadRequest("Not a valid room JID."));
			}
			
			if (!roomService.exists(message.getTo())) {
				throw new ProtocolException(new ItemNotFound());
			}
			
			delegator.processRoomSubjectOrGroupChatMessage(context, message);
			return true;
		} else if (isGroupChatPrivateMessage(message)) {
			delegator.processGroupChatPrivateMessage(context, message);
			return true;
		} else {
			return false;
		}
	}

	private boolean isGroupChatPrivateMessage(Message message) {
		return (message.getType() == null || message.getType() == Message.Type.CHAT) &&
				(message.getTo() != null && !message.getTo().isBareId()) &&
				roomService.exists(message.getTo().getBareId());
	}

}
