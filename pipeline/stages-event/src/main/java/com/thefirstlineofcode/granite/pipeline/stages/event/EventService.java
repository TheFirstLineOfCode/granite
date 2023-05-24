package com.thefirstlineofcode.granite.pipeline.stages.event;

import com.thefirstlineofcode.granite.framework.core.IService;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageReceiver;

@Component("event.service")
public class EventService implements IService {
	@Dependency("event.message.receiver")
	private IMessageReceiver eventMessageReceiver;
	
	@Override
	public void start() throws Exception {
		eventMessageReceiver.start();
	}

	@Override
	public void stop() throws Exception {
		eventMessageReceiver.stop();
	}

}
