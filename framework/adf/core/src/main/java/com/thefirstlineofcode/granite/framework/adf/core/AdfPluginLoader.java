package com.thefirstlineofcode.granite.framework.adf.core;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.pf4j.JarPluginLoader;
import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdfPluginLoader extends JarPluginLoader {
	private static final Logger logger = LoggerFactory.getLogger(AdfPluginLoader.class);
	
	public AdfPluginLoader(AdfPluginManager pluginManager) {
		super(pluginManager);
	}
	
	@Override
	public ClassLoader loadPlugin(Path pluginPath, PluginDescriptor pluginDescriptor) {		
		String pluginId = pluginDescriptor.getPluginId();
		String[] nonPluginDependencyIds = ((AdfPluginManager)pluginManager).getNonPluginDependencyIdsByPluginId(pluginId);
		if (nonPluginDependencyIds == null || nonPluginDependencyIds.length == 0)
			return super.loadPlugin(pluginPath, pluginDescriptor);
		
		File[] nonPluginDependencies = getNonPluginDependenciesByIds(pluginId, nonPluginDependencyIds);
		if (nonPluginDependencies.length == 0)
			return super.loadPlugin(pluginPath, pluginDescriptor);
		
		PluginClassLoader adfClassLoader = new PluginClassLoader(pluginManager, pluginDescriptor,
				getClass().getClassLoader());
		adfClassLoader.addFile(pluginPath.toFile());
		for (File nonPluginDependency : nonPluginDependencies) {
			adfClassLoader.addFile(nonPluginDependency);
		}
		
		return adfClassLoader;
	}

	private File[] getNonPluginDependenciesByIds(String pluginId, String[] nonPluginDependencyIds) {
		List<File> lNonPluginDependencies = new ArrayList<>();
		for (String nonPluginDependencyId : nonPluginDependencyIds) {
			File nonPluginDependency = findNonPluginDependencyByDependencyId(nonPluginDependencyId);
			
			if (nonPluginDependency != null) {
				lNonPluginDependencies.add(nonPluginDependency);
			} else {
				logger.warn("Non-plugin dependency which's id is '{}' not be found. It's needed by plugin: {}.",
						nonPluginDependencyId, pluginId);
			}
		}
		
		return lNonPluginDependencies.toArray(new File[lNonPluginDependencies.size()]);
	}

	private File findNonPluginDependencyByDependencyId(String nonPluginDependencyId) {
		File[] nonPluginDependencies = ((AdfPluginManager)pluginManager).getNonPluginDependencies();
		for (File nonPluginDependency : nonPluginDependencies) {
			if (nonPluginDependency.getName().contains(nonPluginDependencyId)) {
				return nonPluginDependency;
			}
		}
		
		return null;
	}
}
