package com.thefirstlineofcode.granite.framework.core.pipeline.stages.event;

public class EventListenerFactory<E extends IEvent> implements IEventListenerFactory<E> {
	private Class<E> type;
	private IEventListener<E> listener;

	public EventListenerFactory(Class<E> type, IEventListener<E> listener) {
		this.type = type;
		this.listener = listener;
	}
	
	@Override
	public Class<E> getType() {
		return type;
	}

	@Override
	public IEventListener<E> createListener() {
		return listener;
	}

}
