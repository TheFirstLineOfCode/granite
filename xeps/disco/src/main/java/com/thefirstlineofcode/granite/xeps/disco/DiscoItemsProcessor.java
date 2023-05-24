package com.thefirstlineofcode.granite.xeps.disco;

import com.thefirstlineofcode.basalt.xeps.disco.DiscoItems;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IXepProcessor;

public class DiscoItemsProcessor implements IXepProcessor<Iq, DiscoItems> {
	@Dependency("disco.processor")
	private IDiscoProcessor discoProcessor;

	@Override
	public void process(IProcessingContext context, Iq iq, DiscoItems discoItems) {
		discoProcessor.discoItems(context, iq, iq.getTo() == null ? context.getJid() : iq.getTo(), discoItems.getNode());
	}

}
