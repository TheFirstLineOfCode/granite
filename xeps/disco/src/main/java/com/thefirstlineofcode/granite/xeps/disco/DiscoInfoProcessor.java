package com.thefirstlineofcode.granite.xeps.disco;

import com.thefirstlineofcode.basalt.xeps.disco.DiscoInfo;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IXepProcessor;

public class DiscoInfoProcessor implements IXepProcessor<Iq, DiscoInfo> {
	@Dependency("disco.processor")
	private IDiscoProcessor discoProcessor;

	@Override
	public void process(IProcessingContext context, Iq iq, DiscoInfo discoInfo) {
		discoProcessor.discoInfo(context, iq, iq.getTo() == null ? context.getJid() : iq.getTo(), discoInfo.getNode()); 
	}

}
