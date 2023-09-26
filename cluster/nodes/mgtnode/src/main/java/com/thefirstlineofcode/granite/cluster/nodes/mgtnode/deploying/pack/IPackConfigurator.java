package com.thefirstlineofcode.granite.cluster.nodes.mgtnode.deploying.pack;

import com.thefirstlineofcode.granite.cluster.nodes.commons.deploying.DeployPlan;

public interface IPackConfigurator {
	void configure(IPackContext context, DeployPlan deployPlan);
}
