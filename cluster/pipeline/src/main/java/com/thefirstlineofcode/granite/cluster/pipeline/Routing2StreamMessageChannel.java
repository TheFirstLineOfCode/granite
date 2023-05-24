package com.thefirstlineofcode.granite.cluster.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;
import com.thefirstlineofcode.granite.cluster.node.commons.deploying.NodeType;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageChannel;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.IForward;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.IRouter;
import com.thefirstlineofcode.granite.framework.core.repository.IInitializable;

@Component("cluster.routing.2.stream.message.channel")
public class Routing2StreamMessageChannel implements IMessageChannel, IInitializable {
	private static final Logger logger = LoggerFactory.getLogger(Routing2StreamMessageChannel.class);
	
	@Dependency(Constants.DEPENDENCY_ID_NODE_RUNTIME_CONFIGURATION)
	private NodeRuntimeConfiguration nodeRuntimeConfiguration;
	
	@Dependency("router")
	private IRouter router;
	
	private boolean hasStreamAbility;

	@Override
	public void send(IMessage message) {
		JabberId target = (JabberId)message.getHeaders().get(IMessage.KEY_MESSAGE_TARGET);
		if (target == null) {
			Object payload = message.getPayload();
			if (payload instanceof Stanza) {
				target = ((Stanza)payload).getTo();
			}
		}
		
		if (target == null) {
			logger.warn("Null message target. Message content: {}.", message.getPayload());
			return;
		}
		
		IForward[] forwards = router.get(target);
		if (forwards == null || forwards.length == 0) {
			logger.warn("Can't forward message. Message content: {}. Message Target: {}", message.getPayload(), target.toString());
			// TODO Process offline messages.
			return;
		}
		
		for (IForward forward : forwards) {
			forward.to(message);
		}
	}

	@Override
	public void init() {
		NodeType nodeType = nodeRuntimeConfiguration.getDeployPlan().getNodeTypes().get(nodeRuntimeConfiguration.getNodeType());
		hasStreamAbility = nodeType.hasAbility("stream");
		
		if (!hasStreamAbility) {
			throw new RuntimeException("Splitting application vertically isn't supported yet. Appnode must possess all IM abilities(stream, processing, event).");
		}
	}

}
