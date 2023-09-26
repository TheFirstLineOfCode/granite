package com.thefirstlineofcode.granite.cluster.nodes.mgtnode.deploying.pack;

import com.thefirstlineofcode.granite.cluster.nodes.commons.deploying.DeployPlan;

public interface IAppnodeRuntimesPacker {
	void pack(String nodeTypeName, String runtimeName, DeployPlan configuration);
}
