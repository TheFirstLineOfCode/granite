package com.thefirstlineofcode.granite.lite.pipeline;

import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageChannel;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageConnector;
import com.thefirstlineofcode.granite.framework.core.repository.IComponentIdAware;
import com.thefirstlineofcode.granite.framework.core.repository.IRepository;
import com.thefirstlineofcode.granite.framework.core.repository.IRepositoryAware;

@Component(value="lite.stream.2.parsing.message.channel",
	aliases={
			"lite.parsing.2.processing.message.channel",
			"lite.any.2.event.message.channel",
			"lite.any.2.routing.message.channel",
			"lite.routing.2.stream.message.channel"
		}
)
public class MessageChannel implements IMessageChannel, IComponentIdAware, IRepositoryAware {
	protected String connectorComponentId;
	protected IRepository repository;
	
	private volatile IMessageConnector connector;
	
	@Override
	public void send(IMessage message) {
		getConnector().put(message);
	}

	private IMessageConnector getConnector() {
		if (connector != null)
			return connector;
		
		return getConnectorByComponentId();
	}

	protected synchronized IMessageConnector getConnectorByComponentId() {
		if (connector != null)
			return connector;
		
		connector = (IMessageConnector)repository.get(connectorComponentId);
		if (connector == null)
			throw new RuntimeException(String.format("Can't get connector by component ID: %s.", connectorComponentId));
		
		return connector;
	}
	
	@Override
	public void setComponentId(String componentId) {
		if (connectorComponentId == null) {
			connectorComponentId = componentId.substring(0, componentId.length() - 8) + ".receiver";
		}
	}

	@Override
	public void setRepository(IRepository repository) {
		this.repository = repository;
	}

}
