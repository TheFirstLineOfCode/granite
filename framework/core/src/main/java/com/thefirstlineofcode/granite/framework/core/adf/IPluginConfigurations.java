package com.thefirstlineofcode.granite.framework.core.adf;

import com.thefirstlineofcode.granite.framework.core.config.IConfiguration;

public interface IPluginConfigurations {
	IConfiguration getConfiguration(String pluginId);
}