package com.thefirstlineofcode.granite.framework.core.pipeline.stages.event;

import com.thefirstlineofcode.granite.framework.core.pipeline.stages.IPipelineExtender;

public interface IEventListenerFactory<E extends IEvent> extends IPipelineExtender {
	Class<E> getType();
	IEventListener<E> createListener();
}
