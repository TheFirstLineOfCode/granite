package com.thefirstlineofcode.granite.framework.im;

import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;

public interface IPresenceProcessor {
	boolean process(IProcessingContext context, Presence presence);
}
