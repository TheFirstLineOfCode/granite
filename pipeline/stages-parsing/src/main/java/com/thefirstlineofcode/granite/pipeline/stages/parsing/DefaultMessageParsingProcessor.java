package com.thefirstlineofcode.granite.pipeline.stages.parsing;

import com.thefirstlineofcode.basalt.oxm.parsers.im.MessageParserFactory;
import com.thefirstlineofcode.basalt.oxm.parsers.im.PresenceParserFactory;
import com.thefirstlineofcode.basalt.xmpp.core.MessageProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.PresenceProtocolChain;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;

@Component("default.message.parsing.processor")
public class DefaultMessageParsingProcessor extends MinimumMessageParsingProcessor {
	@Override
	protected void registerPredefinedParsers() {
		super.registerPredefinedParsers();
		
		parsingFactory.register(new PresenceProtocolChain(), new PresenceParserFactory());
		parsingFactory.register(new MessageProtocolChain(), new MessageParserFactory());
	}
	
}
