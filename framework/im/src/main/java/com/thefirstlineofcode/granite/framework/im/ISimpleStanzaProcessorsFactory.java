package com.thefirstlineofcode.granite.framework.im;

import org.pf4j.ExtensionPoint;

public interface ISimpleStanzaProcessorsFactory extends ExtensionPoint {
	IPresenceProcessor[] getPresenceProcessors();
	IMessageProcessor[] getMessageProcessors();
}
