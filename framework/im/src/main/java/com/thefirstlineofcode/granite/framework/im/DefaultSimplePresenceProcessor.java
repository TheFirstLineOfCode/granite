package com.thefirstlineofcode.granite.framework.im;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.framework.core.utils.OrderComparator;

public class DefaultSimplePresenceProcessor implements IPresenceProcessor {
	private List<IPresenceProcessor> presenceProcessors;
	
	public DefaultSimplePresenceProcessor() {
		presenceProcessors = new ArrayList<>();
	}
	
	public void setPresenceProcessors(List<IPresenceProcessor> presenceProcessors) {
		this.presenceProcessors = presenceProcessors;
		Collections.sort(presenceProcessors, new OrderComparator<>());
	}
	
	public void addPresenceProcessor(IPresenceProcessor presenceProcessor) {
		if (!presenceProcessors.contains(presenceProcessor)) {
			presenceProcessors.add(presenceProcessor);
			Collections.sort(presenceProcessors, new OrderComparator<>());
		}
	}
	
	@Override
	public boolean process(IProcessingContext context, Presence presence) {
		for (IPresenceProcessor presenceProcessor : presenceProcessors) {
			if (presenceProcessor.process(context, presence))
				return true;
		}
		
		return false;
	}

}
