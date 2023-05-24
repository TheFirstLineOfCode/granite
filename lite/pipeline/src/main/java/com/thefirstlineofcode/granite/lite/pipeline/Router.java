package com.thefirstlineofcode.granite.lite.pipeline;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.IForward;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.IRouter;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.RoutingRegistrationException;

@Component("lite.router")
public class Router implements IRouter {

	@Override
	public void register(JabberId jid, String localNodeId) throws RoutingRegistrationException {
		// do nothing
	}

	@Override
	public void unregister(JabberId jid) throws RoutingRegistrationException {
		// do nothing
	}

	@Override
	public IForward[] get(JabberId jid) {
		// this method is never called
		return null;
	}

}
