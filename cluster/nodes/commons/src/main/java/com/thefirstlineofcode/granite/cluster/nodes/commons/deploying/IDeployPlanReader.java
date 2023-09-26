package com.thefirstlineofcode.granite.cluster.nodes.commons.deploying;

import java.nio.file.Path;

public interface IDeployPlanReader {
	DeployPlan read(Path deployPlanPath) throws DeployPlanException;
}
