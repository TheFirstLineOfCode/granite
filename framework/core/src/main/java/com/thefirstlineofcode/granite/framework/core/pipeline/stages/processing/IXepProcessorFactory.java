package com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing;

import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.IPipelineExtender;

public interface IXepProcessorFactory<S extends Stanza, X> extends IPipelineExtender {
	ProtocolChain getProtocolChain();
	IXepProcessor<S, X> createProcessor() throws Exception;
	boolean isSingleton();
}
