package com.thefirstlineofcode.granite.cluster.node.commons.deploying;

import java.nio.file.Path;

public interface IDeployPlanReader {
	DeployPlan read(Path deployPlanPath) throws DeployPlanException;
}
