package com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack;

import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlan;

public interface IPackConfigurator {
	void configure(IPackContext context, DeployPlan deployPlan);
}
