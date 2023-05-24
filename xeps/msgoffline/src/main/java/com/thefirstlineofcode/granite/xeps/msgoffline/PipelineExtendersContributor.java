package com.thefirstlineofcode.granite.xeps.msgoffline;

import org.pf4j.Extension;

import com.thefirstlineofcode.granite.framework.core.pipeline.stages.IPipelineExtendersConfigurator;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.PipelineExtendersConfigurator;
import com.thefirstlineofcode.granite.framework.im.OfflineMessageEvent;
import com.thefirstlineofcode.granite.framework.im.ResourceAvailabledEvent;

@Extension
public class PipelineExtendersContributor extends PipelineExtendersConfigurator {
	@Override
	protected void configure(IPipelineExtendersConfigurator configurator) {
		configurator.
			registerEventListener(ResourceAvailabledEvent.class, new ResourceAvailabledListener()).
			registerEventListener(OfflineMessageEvent.class, new OfflineMessageListener());
	}
}
