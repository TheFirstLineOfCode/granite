package com.thefirstlineofcode.granite.framework.core.pipeline.stages.event;


public interface IEventListener<T extends IEvent> {
	void process(IEventContext context, T event);
}
