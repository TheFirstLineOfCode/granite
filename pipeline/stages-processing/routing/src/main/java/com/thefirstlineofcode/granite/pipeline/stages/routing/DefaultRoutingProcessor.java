package com.thefirstlineofcode.granite.pipeline.stages.routing;

import com.thefirstlineofcode.basalt.oxm.translators.im.MessageTranslatorFactory;
import com.thefirstlineofcode.basalt.oxm.translators.im.PresenceTranslatorFactory;
import com.thefirstlineofcode.basalt.protocol.im.stanza.Message;
import com.thefirstlineofcode.basalt.protocol.im.stanza.Presence;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;

@Component("default.routing.processor")
public class DefaultRoutingProcessor extends MinimumRoutingProcessor {
	
	@Override
	protected void registerPredefinedTranslators() {
		super.registerPredefinedTranslators();
		
		translatingFactory.register(Presence.class, new PresenceTranslatorFactory());
		translatingFactory.register(Message.class, new MessageTranslatorFactory());
	}
}
