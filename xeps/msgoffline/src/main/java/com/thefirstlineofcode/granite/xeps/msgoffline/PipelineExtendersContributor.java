package com.thefirstlineofcode.granite.xeps.msgoffline;

import org.pf4j.Extension;

import com.thefirstlineofcode.granite.framework.core.pipeline.stages.IPipelineExtendersConfigurator;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.PipelineExtendersConfigurator;
import com.thefirstlineofcode.granite.framework.im.OfflineMessageEvent;
import com.thefirstlineofcode.granite.framework.im.ResourceAvailableEvent;

@Extension
public class PipelineExtendersContributor extends PipelineExtendersConfigurator {
	@Override
	protected void configure(IPipelineExtendersConfigurator configurator) {
		configurator.
			registerEventListener(ResourceAvailableEvent.class, new ResourceAvailableListener()).
			registerEventListener(OfflineMessageEvent.class, new OfflineMessageListener());
	}
}
