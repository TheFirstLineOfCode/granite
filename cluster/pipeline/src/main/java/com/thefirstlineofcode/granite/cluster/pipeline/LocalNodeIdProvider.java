package com.thefirstlineofcode.granite.cluster.pipeline;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.ILocalNodeIdProvider;

@Component("cluster.local.node.id.provider")
public class LocalNodeIdProvider implements ILocalNodeIdProvider {
	private static final Logger logger = LoggerFactory.getLogger(LocalNodeIdProvider.class);
	
	@Dependency("ignite")
	private Ignite ignite;
	
	private volatile String localNodeId;
	
	@Override
	public String getLocalNodeId() {
		if (localNodeId != null)
			return localNodeId;
		
		synchronized (this) {
			if (localNodeId != null)
				return localNodeId;
			
			localNodeId = Long.toString(ignite.atomicSequence("node-sequence", 0, true).getAndAdd(1));
			logger.info("Cluster node '{}' is assigned to a local node ID: {}.", ignite.cluster().localNode().id().toString(), localNodeId);
		}
		
		return localNodeId;
	}

}
