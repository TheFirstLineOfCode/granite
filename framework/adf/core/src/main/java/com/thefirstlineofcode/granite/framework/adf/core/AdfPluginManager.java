package com.thefirstlineofcode.granite.framework.adf.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.pf4j.CompoundPluginLoader;
import org.pf4j.DefaultExtensionFactory;
import org.pf4j.DefaultPluginLoader;
import org.pf4j.DefaultPluginManager;
import org.pf4j.DevelopmentPluginLoader;
import org.pf4j.ExtensionFactory;
import org.pf4j.PluginLoader;

import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentServiceAware;

public class AdfPluginManager extends DefaultPluginManager implements IApplicationComponentServiceAware {
	private static final String FILE_NAME_PLUGIN_PROPERTIES = "plugin.properties";
	private static final String CHAR_COMMA = ",";
	private static final String PROPERTY_NAME_PLUGIN_ID = "plugin.id";
	private static final String PROPERTY_NAME_NON_PLUGIN_DEPENDENCIES = "non-plugin.dependencies";
	private AdfComponentService appComponentService;
	private File[] nonPluginDependencies;
	private Map<String, String[]> pluginIdToNonPluginDependencyIds;
	
	public AdfPluginManager(Path pluginsRoot) {
		super(pluginsRoot);
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		
		List<File> lNonPluginDependencies = new ArrayList<>();
		pluginIdToNonPluginDependencyIds = new HashMap<>();
		File pluginsDir = getPluginsRoot().toFile();
		
		for (File file : pluginsDir.listFiles()) {
			if (!file.getName().endsWith(".jar"))
				continue;
			
			try {				
				if (!isPlugin(file)) {
					lNonPluginDependencies.add(file);
				}
			} catch (Exception e) {
				throw new RuntimeException("Can't load non-plugin dependencies from plugins directory.", e);
			}
		}
		
		nonPluginDependencies = lNonPluginDependencies.toArray(new File[lNonPluginDependencies.size()]);
	}
	
	private boolean isPlugin(File file) throws Exception {
		JarFile jarFile = null;
		try {
			jarFile = new JarFile(file);
			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (FILE_NAME_PLUGIN_PROPERTIES.equals(entry.getName())) {
					readPluginIdAndNonPluginDepdencies(jarFile, entry);
					return true;
				}
			}
		} finally {
			if (jarFile != null)
				jarFile.close();
		}
		
		return false;
	}

	private void readPluginIdAndNonPluginDepdencies(JarFile jarFile, JarEntry entry) {
		try {
			Properties properties = new Properties();
			properties.load(jarFile.getInputStream(entry));
			String sNonPluginDependencies = (String)properties.get(PROPERTY_NAME_NON_PLUGIN_DEPENDENCIES);
			if (sNonPluginDependencies != null) {
				String pluginId = properties.getProperty(PROPERTY_NAME_PLUGIN_ID);
				
				StringTokenizer st = new StringTokenizer(sNonPluginDependencies, CHAR_COMMA);
				String[] nonPluginDependencyIds = new String[st.countTokens()];
				for (int i = 0; i < nonPluginDependencyIds.length; i++) {
					nonPluginDependencyIds[i] = st.nextToken().trim();
				}
				
				pluginIdToNonPluginDependencyIds.put(pluginId, nonPluginDependencyIds);
			}
		} catch (IOException e) {
			throw new RuntimeException(String.format("Can't read non-plugin dependencies. Plugin file: %s", jarFile.getName()), e);
		}
	}
	
	@Override
	protected ExtensionFactory createExtensionFactory() {
		return new AdfExtensionFactory();
	}
	
	private class AdfExtensionFactory extends DefaultExtensionFactory {
		@Override
		public <T> T create(Class<T> extensionClass) {
			T extension = super.create(extensionClass);
			if (extension != null)
				appComponentService.inject(extension);
			
			return extension;
		}
	}
	
	@Override
	public void setApplicationComponentService(IApplicationComponentService appComponentService) {
		if (appComponentService instanceof AdfComponentService) {
			this.appComponentService = (AdfComponentService)appComponentService;
		} else {
			throw new IllegalArgumentException(String.format("Class %s Not an ADF plugin manager.", appComponentService.getClass().getName()));
		}
	}
	
	@Override
	protected PluginLoader createPluginLoader() {
		return new CompoundPluginLoader().
				add(new DevelopmentPluginLoader(this), this::isDevelopment).
				add(new AdfPluginLoader(this), this::isNotDevelopment).
				add(new DefaultPluginLoader(this), this::isNotDevelopment);
	}
	
	public File[] getNonPluginDependencies() {
		return nonPluginDependencies;
	}
	
	public String[] getNonPluginDependencyIdsByPluginId(String pluginId) {
		return pluginIdToNonPluginDependencyIds.get(pluginId);
	}
}
