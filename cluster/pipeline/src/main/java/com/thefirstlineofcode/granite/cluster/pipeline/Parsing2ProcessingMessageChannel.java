package com.thefirstlineofcode.granite.cluster.pipeline;

import com.thefirstlineofcode.granite.cluster.node.commons.deploying.NodeType;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageChannel;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageConnector;
import com.thefirstlineofcode.granite.framework.core.repository.IInitializable;

@Component("cluster.parsing.2.processing.message.channel")
public class Parsing2ProcessingMessageChannel implements IMessageChannel, IInitializable {
	@Dependency("connector")
	private IMessageConnector connector;
	
	@Dependency(Constants.DEPENDENCY_ID_NODE_RUNTIME_CONFIGURATION)
	private NodeRuntimeConfiguration nodeRuntimeConfiguration;
	
	private boolean hasProcessingAbility;

	@Override
	public void send(IMessage message) {
		connector.put(message);
	}
	
	@Override
	public void init() {
		NodeType nodeType = nodeRuntimeConfiguration.getDeployPlan().getNodeTypes().get(nodeRuntimeConfiguration.getNodeType());
		hasProcessingAbility = nodeType.hasAbility("processing");
		
		if (!hasProcessingAbility) {
			throw new RuntimeException("Splitting application vertically isn't supported yet. Appnode must possess all IM abilities(stream, processing, event).");
		}
	}
}
