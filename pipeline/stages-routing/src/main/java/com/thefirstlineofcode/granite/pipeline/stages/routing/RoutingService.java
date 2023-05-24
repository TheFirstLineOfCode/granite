package com.thefirstlineofcode.granite.pipeline.stages.routing;

import com.thefirstlineofcode.granite.framework.core.IService;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageReceiver;

@Component("routing.service")
public class RoutingService implements IService {
	@Dependency("routing.message.receiver")
	private IMessageReceiver routingMessageReceiver;
	
	@Override
	public void start() throws Exception {
		if (routingMessageReceiver != null)
			routingMessageReceiver.start();
	}

	@Override
	public void stop() throws Exception {
		if (routingMessageReceiver != null)
			routingMessageReceiver.stop();
	}
}
