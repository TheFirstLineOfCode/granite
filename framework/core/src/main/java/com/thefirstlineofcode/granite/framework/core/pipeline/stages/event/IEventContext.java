package com.thefirstlineofcode.granite.framework.core.pipeline.stages.event;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;

public interface IEventContext {
	void write(Stanza stanza);
	void write(JabberId target, Stanza stanza);
	void write(JabberId target, String message);
}
