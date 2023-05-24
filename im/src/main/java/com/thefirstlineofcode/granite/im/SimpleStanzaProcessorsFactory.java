package com.thefirstlineofcode.granite.im;

import org.pf4j.Extension;

import com.thefirstlineofcode.granite.framework.im.IMessageProcessor;
import com.thefirstlineofcode.granite.framework.im.IPresenceProcessor;
import com.thefirstlineofcode.granite.framework.im.ISimpleStanzaProcessorsFactory;

@Extension
public class SimpleStanzaProcessorsFactory implements ISimpleStanzaProcessorsFactory {

	@Override
	public IPresenceProcessor[] getPresenceProcessors() {
		return new IPresenceProcessor[] {
			new StandardPresenceProcessor(),
			new SubscriptionProcessor()
		};
	}

	@Override
	public IMessageProcessor[] getMessageProcessors() {
		return new IMessageProcessor[] {
			new StandardMessageProcessor()
		};
	}

}
