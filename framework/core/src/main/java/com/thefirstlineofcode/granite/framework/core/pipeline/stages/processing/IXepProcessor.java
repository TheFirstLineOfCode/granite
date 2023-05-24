package com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing;

import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;

public interface IXepProcessor<S extends Stanza, X> {
	void process(IProcessingContext context, S stanza, X xep);
}
