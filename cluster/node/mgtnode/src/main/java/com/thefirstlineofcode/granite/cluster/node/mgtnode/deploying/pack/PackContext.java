package com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack;

import java.io.File;
import java.util.Map;

import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlan;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.config.ConfigManager;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.config.IConfigManager;

public class PackContext implements IPackContext {
	private File configurationDir;
	private File runtimeConfigurationDir;
	private File repositoryDir;
	private File runtimeDir;
	private File runtimeLibsDir;
	private File runtimePluginsDir;
	private Map<String, IPackModule> packModules;
	private String nodeType;
	private DeployPlan deployPlan;
	private IConfigManager configManager;
	
	public PackContext(File configurationDir, File repositoryDirPath, File runtimeDir,
			File runtimeLibsDir, File runtimePluginsDir, File runtimeConfigurationDir,
			Map<String, IPackModule> packModules, String nodeType, DeployPlan deployPlan) {
		this.configurationDir = configurationDir;
		this.repositoryDir = repositoryDirPath;
		this.runtimeDir = runtimeDir;
		this.runtimeLibsDir = runtimeLibsDir;
		this.runtimePluginsDir = runtimePluginsDir;
		this.runtimeConfigurationDir = runtimeConfigurationDir;
		this.packModules = packModules;
		this.nodeType = nodeType;
		this.deployPlan = deployPlan;
		
		configManager = new ConfigManager();
	}

	@Override
	public IPackModule getPackModule(String moduleName) {
		return packModules.get(moduleName);
	}

	@Override
	public File getRepositoryDir() {
		return repositoryDir;
	}

	@Override
	public File getRuntimePluginsDir() {
		return runtimePluginsDir;
	}

	@Override
	public IConfigManager getConfigManager() {
		return configManager;
	}
	
	@Override
	public String getNodeType() {
		return nodeType;
	}
	
	@Override
	public DeployPlan getDeployPlan() {
		return deployPlan;
	}

	@Override
	public File getRuntimeDir() {
		return runtimeDir;
	}

	@Override
	public File getConfigurationDir() {
		return configurationDir;
	}

	@Override
	public File getRuntimeConfigurationDir() {
		return runtimeConfigurationDir;
	}

	@Override
	public File getRuntimeLibsDir() {
		return runtimeLibsDir;
	}
	
}
