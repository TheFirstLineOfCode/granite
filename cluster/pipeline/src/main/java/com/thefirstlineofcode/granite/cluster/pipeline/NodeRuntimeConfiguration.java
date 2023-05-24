package com.thefirstlineofcode.granite.cluster.pipeline;

import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlan;

public class NodeRuntimeConfiguration {
	private String nodeType;
	private DeployPlan deployPlan;
	
	public NodeRuntimeConfiguration(String nodeType, DeployPlan deployPlan) {
		this.nodeType = nodeType;
		this.deployPlan = deployPlan;
	}

	public String getNodeType() {
		return nodeType;
	}
	
	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}
	
	public DeployPlan getDeployPlan() {
		return deployPlan;
	}
	
	public void setDeployPlan(DeployPlan deployPlan) {
		this.deployPlan = deployPlan;
	}
	
}
