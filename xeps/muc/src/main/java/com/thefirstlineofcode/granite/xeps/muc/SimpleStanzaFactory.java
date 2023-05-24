package com.thefirstlineofcode.granite.xeps.muc;

import org.pf4j.Extension;

import com.thefirstlineofcode.granite.framework.im.IMessageProcessor;
import com.thefirstlineofcode.granite.framework.im.IPresenceProcessor;
import com.thefirstlineofcode.granite.framework.im.ISimpleStanzaProcessorsFactory;

@Extension
public class SimpleStanzaFactory implements ISimpleStanzaProcessorsFactory {

	@Override
	public IPresenceProcessor[] getPresenceProcessors() {
		return new IPresenceProcessor[] {
			new GroupChatPresenceProcessor()
		};
	}

	@Override
	public IMessageProcessor[] getMessageProcessors() {
		return new IMessageProcessor[] {
			new GroupChatMessageProcessor()
		};
	}

}
