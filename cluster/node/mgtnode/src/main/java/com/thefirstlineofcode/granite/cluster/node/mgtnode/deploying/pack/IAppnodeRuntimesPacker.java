package com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack;

import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlan;

public interface IAppnodeRuntimesPacker {
	void pack(String nodeTypeName, String runtimeName, DeployPlan configuration);
}
