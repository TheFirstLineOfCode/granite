package com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager implements IConfigManager {
	private List<IConfig> configs;
	
	public ConfigManager() {
		configs = new ArrayList<>();
	}
	
	@Override
	public IConfig createOrGetConfig(Path parentPath, String configFileName) {
		if (parentPath == null)
			throw new IllegalArgumentException("Null parent path.");
		
		if (configFileName == null)
			throw new IllegalArgumentException("Null config file name.");
		
		Path configPath = parentPath.resolve(configFileName);
		IConfig config = getConfig(configPath);
		if (config == null) {
			config = new Config(configPath);
			configs.add(config);
		}
		
		return config;
	}

	private IConfig getConfig(Path configPath) {
		for (IConfig config : configs) {
			if (config.getConfigPath().equals(configPath))
				return config;
		}
		
		return null;
	}

	@Override
	public void saveConfigs() {
		for (IConfig config : configs) {
			config.save();
		}
	}

}
