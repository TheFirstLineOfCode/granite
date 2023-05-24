package com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack;

import java.io.File;

import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlan;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.config.IConfigManager;

public interface IPackContext {
	File getConfigurationDir();
	File getRuntimeConfigurationDir();
	File getRepositoryDir();
	File getRuntimeDir();
	File getRuntimeLibsDir();
	File getRuntimePluginsDir();
	String getNodeType();
	DeployPlan getDeployPlan();
	IPackModule getPackModule(String moduleName);
	IConfigManager getConfigManager();
}
