package com.thefirstlineofcode.granite.framework.core.adf;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.thefirstlineofcode.basalt.oxm.SectionalProperties;
import com.thefirstlineofcode.granite.framework.core.config.DummyConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.PropertiesConfiguration;


public class PluginConfigurations implements IPluginConfigurations {
	private static final String CONFIGURATION_FILE = "plugins.ini";
	
	private Map<String, PropertiesConfiguration> configurations;
	
	public PluginConfigurations(String configDir) {
		File configFile = new File(configDir + "/" + CONFIGURATION_FILE);
		SectionalProperties sp = new SectionalProperties();
		if (configFile.exists()) {
			InputStream inputStream = null;
			try {
				inputStream = new BufferedInputStream(new FileInputStream(configFile));
				sp.load(inputStream);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		readConfigurations(sp);
	}
	
	private void readConfigurations(SectionalProperties sp) {
		configurations = new HashMap<>();
		for (String sectionName : sp.getSectionNames()) {
			String pluginId = sectionName;
			PropertiesConfiguration configuration = new PropertiesConfiguration(sp.getSection(pluginId));
			configurations.put(pluginId, configuration);
		}
	}
	
	@Override
	public IConfiguration getConfiguration(String pluginId) {
		IConfiguration configuration = configurations.get(pluginId);
		
		if (configuration == null) {
			configuration = new DummyConfiguration();
		}
		
		return configuration;
	}
}
