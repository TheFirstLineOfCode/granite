package com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlan;
import com.thefirstlineofcode.granite.cluster.node.commons.deploying.NodeType;
import com.thefirstlineofcode.granite.cluster.node.commons.utils.IoUtils;
import com.thefirstlineofcode.granite.cluster.node.commons.utils.SectionalProperties;
import com.thefirstlineofcode.granite.cluster.node.commons.utils.StringUtils;
import com.thefirstlineofcode.granite.cluster.node.commons.utils.TargetExistsException;
import com.thefirstlineofcode.granite.cluster.node.commons.utils.ZipUtils;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.Options;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.IPackModule.Scope;

public class AppnodeRuntimesPacker implements IAppnodeRuntimesPacker {
	private static final String NAME_PREFIX_GRANITE_SERVER = "granite-server-";
	private static final String SCOPE_PLUGIN = "plugin";
	private static final String SCOPE_SYSTEM = "system";
	private static final String CONFIGURATION_KEY_SCOPE = "scope";
	private static final String DIRECTORY_NAME_LIBS = "libs";
	private static final String DIRECTORY_NAME_PLUGINS = "plugins";
	private static final String DIRECTORY_NAME_CONFIGURATION = "configuration";
	private static final String DIRECTORY_NAME_PACK_TMP = "pack-tmp";
	private static final String CONFIGURATION_KEY_CONFIGURATOR = "configurator";
	private static final String CONFIGURATION_KEY_LIBRARIES = "libraries";
	private static final String CONFIGURATION_KEY_DEPENDED = "depended";
	private static final String NAME_PREFIX_PROTOCOL_MODULE = "protocol-";
	private static final String NAME_PREFIX_ABILITY_MODULE = "ability-";
	private static final String FILE_NAME_PACK_MODULES_CONFIG = "pack_modules.ini";
	private static final String RESOURCE_NAME_PACK_MODULES_CONFIG = "META-INF/com/thefirstlineofcode/granite/pack_modules.ini";
	
	private static final Logger logger = LoggerFactory.getLogger(AppnodeRuntimesPacker.class);
	
	private Options options;
	private boolean packModulesLoaded;
	private Map<String, IPackModule> packModules;
	
	public AppnodeRuntimesPacker(Options options) {
		this.options = options;
		packModulesLoaded = false;
		packModules = new HashMap<>();
	}

	@Override
	public void pack(String nodeType, String runtimeName, DeployPlan deployPlan) {
		File runtimeZip = new File(new File(options.getAppnodeRuntimesDir()), runtimeName + ".zip");
		if (runtimeZip.exists()) {
			if (!options.isRepack()) {
				logger.info("Runtime {} has already existed. Packing is ignored. Use -repack option if you want to repack all runtimes anyway.", runtimeName);
				return;
			}
			
			try {
				logger.info("Runtime {} existed. Deleting it...", runtimeName);
				Files.delete(runtimeZip.toPath());
			} catch (IOException e) {
				throw new RuntimeException(String.format("Can't delete runtime zip file %s.", runtimeZip.getPath()), e);
			}
		}
		
		File packTmpDir = new File(options.getAppnodeRuntimesDir(), DIRECTORY_NAME_PACK_TMP);
		try {
			doPack(nodeType, runtimeName, deployPlan, new File(options.getAppnodeRuntimesDir()), packTmpDir);
		} catch (IOException e) {
			throw new RuntimeException(String.format("Can't pack appnode runtime %s.", runtimeName), e);
		} finally {
			if (packTmpDir != null && packTmpDir.exists()) {
				logger.debug("Removing pack temporary directory...");
				IoUtils.deleteFileRecursively(packTmpDir);
			}
		}
	}

	private void doPack(String nodeType, String runtimeName, DeployPlan deployPlan,
			File runtimesDir, File packTmpDir) throws IOException {
		if (!isPackModulesLoaded()) {
			logger.debug("Ready to load pack modules.");
			loadPackModules();
			logger.debug("Pack modules loaded.");
		}
		
		if (!packTmpDir.exists()) {
			logger.debug("Pack temporary directory doesn't exist. Creating it...");
			Files.createDirectories(packTmpDir.toPath());
		}
		
		Path runtimeDirPath = createChildDir(packTmpDir, runtimeName);
		Path runtimeLibsDirPath = createChildDir(runtimeDirPath.toFile(), DIRECTORY_NAME_LIBS);
		Path runtimePluginsDirPath = createChildDir(runtimeDirPath.toFile(), DIRECTORY_NAME_PLUGINS);
		Path runtimeConfigurationDirPath = createChildDir(runtimeDirPath.toFile(), DIRECTORY_NAME_CONFIGURATION);
		
		IPackContext context = createContext(
				new File(options.getConfigurationDir()), new File(options.getRepositoryDir()),
				runtimeDirPath.toFile(), runtimeLibsDirPath.toFile(), runtimePluginsDirPath.toFile(),
				runtimeConfigurationDirPath.toFile(), packModules, nodeType, deployPlan);
		NodeType node = deployPlan.getNodeTypes().get(nodeType);
		
		logger.debug("Packing node {}.", nodeType);
		
		File repositoryDir = new File(options.getRepositoryDir());
		copyGraniteServerJar(repositoryDir, runtimeDirPath);
		copyPackModules(context, node, deployPlan);
		configure(context, node, deployPlan);
		
		zipRuntime(packTmpDir, runtimesDir, runtimeName);
	}
	
	private void copyGraniteServerJar(File repoistoryDir, Path runtimeDirPath) {
		for (File file : repoistoryDir.listFiles()) {
			if (file.getName().startsWith(NAME_PREFIX_GRANITE_SERVER)) {
				try {
					Files.copy(file.toPath(), runtimeDirPath.resolve(file.getName()), StandardCopyOption.COPY_ATTRIBUTES,
							StandardCopyOption.REPLACE_EXISTING);
					return;
				} catch (IOException e) {
					throw new RuntimeException("Failed to copy granite server jar.", e);
				}	
			}
		}
		
		throw new RuntimeException("Can't find granite server jar to copy.");		
	}

	private void zipRuntime(File packTmpDir, File runtimesDir, String runtimeName) throws IOException {
		File runtimeZip = new File(runtimesDir, runtimeName + ".zip");
		if (runtimeZip.exists()) {
			Files.delete(runtimeZip.toPath());
		}
		
		try {
			ZipUtils.zip(packTmpDir, runtimeZip);
		} catch (TargetExistsException e) {
			// ??? is it impossible?
			throw new RuntimeException("Runtime zip has alread existed.", e);
		}
	}

	private Path createChildDir(File parentDir, String name) {
		File childDir = new File(parentDir, name);
		
		try {
			return Files.createDirectory(childDir.toPath());
		} catch (IOException e) {
			throw new RuntimeException(String.format("Can't create directory %s.", childDir), e);
		}
	}

	private void configure(IPackContext context, NodeType node, DeployPlan deployPlan) {
		for (String abilityName : node.getAbilities()) {
			IPackModule module = packModules.get(NAME_PREFIX_ABILITY_MODULE + abilityName);
			module.configure(context, deployPlan);
		}
		
		for (String protocolName : node.getProtocols()) {
			IPackModule module = packModules.get(NAME_PREFIX_PROTOCOL_MODULE + protocolName);
			module.configure(context, deployPlan);
		}
		
		context.getConfigManager().saveConfigs();
	}

	private void copyPackModules(IPackContext context, NodeType node, DeployPlan deployPlan) {
		for (String abilityName : node.getAbilities()) {
			logger.info("Copying ability[{}] libraries...", abilityName);
			IPackModule module = packModules.get(NAME_PREFIX_ABILITY_MODULE + abilityName);
			module.copyLibraries(context);
		}
		
		if (node.getProtocols() != null) {
			for (String protocolName : node.getProtocols()) {
				logger.info("Copying protocol[{}] libraries...", protocolName);
				IPackModule module = packModules.get(NAME_PREFIX_PROTOCOL_MODULE + protocolName);
				module.copyLibraries(context);
			}
		}
	}

	private IPackContext createContext(File configDir, File repositoryDir, File runtimeDir, File libsDir,
			File pluginsDir, File configurationDir, Map<String, IPackModule> packModules,
				String nodeType, DeployPlan deployPlan) {
		return new PackContext(configDir, repositoryDir, runtimeDir,
				libsDir, pluginsDir, configurationDir, packModules, nodeType, deployPlan);
	}
	
	private void loadPackModules() {
		InputStream packModulesConfigInputStream = null; 
		
		File packModulesConfigFile = new File(options.getConfigurationDir(), FILE_NAME_PACK_MODULES_CONFIG);
		if (packModulesConfigFile.exists()) {
			try {
				packModulesConfigInputStream = new FileInputStream(packModulesConfigFile);
			} catch (FileNotFoundException e) {
				// ignore. try load pack modules configuration file from jar resource.
			}
		}
		
		if (packModulesConfigInputStream == null) {
			URL resource = getClass().getClassLoader().getResource(RESOURCE_NAME_PACK_MODULES_CONFIG);
			if (resource == null)
				throw new RuntimeException("Can't get pack_modules.ini.");
			
			try {
				packModulesConfigInputStream = resource.openStream();
			} catch (IOException e) {
				throw new IllegalArgumentException("Can't load pack_modules.ini.", e);
			}
		}
		
		SectionalProperties sp = new SectionalProperties();
		try {
			sp.load(packModulesConfigInputStream);
		} catch (IOException e) {
			throw new IllegalArgumentException("Can't load pack_modules.ini.", e);
		}
		
		for (String sectionName : sp.getSectionNames()) {
			Properties properties = sp.getSection(sectionName);
			
			String sScope = SCOPE_PLUGIN;
			sScope = properties.getProperty(CONFIGURATION_KEY_SCOPE);
			
			Scope scope = getScope(sScope);
			String[] dependedModules = getDependedModules(StringUtils.stringToArray((String)properties.getProperty(CONFIGURATION_KEY_DEPENDED)));
			CopyLibraryOperation[] copyLibraries = getCopyLibraryOperations(scope, StringUtils.stringToArray(properties.getProperty(CONFIGURATION_KEY_LIBRARIES)));
			IPackConfigurator configurator = getConfigurator(properties.getProperty(CONFIGURATION_KEY_CONFIGURATOR));
			
			packModules.put(sectionName, new PackModule(sectionName, scope, dependedModules, copyLibraries, configurator));
		}
		
	}

	private Scope getScope(String sScope) {
		if (sScope == null)
			return Scope.PLUGIN;
		
		if (SCOPE_SYSTEM.equals(sScope))
			return Scope.SYSTEM;
		
		if (!SCOPE_PLUGIN.equals(sScope))
			throw new IllegalArgumentException("Illegal pack module scope: " + sScope);
		
		
		return Scope.PLUGIN;
	}

	private String[] getDependedModules(String[] dependedModules) {
		return (dependedModules == null || dependedModules.length == 0) ? null : dependedModules;
	}

	private IPackConfigurator getConfigurator(String sConfigurator) {
		if (sConfigurator == null)
			return null;
		
		try {
			Class<?> configuratorClass = Class.forName(sConfigurator);
			
			if (!IPackConfigurator.class.isAssignableFrom(configuratorClass)) {
				throw new IllegalArgumentException(String.format("Pack configurator %s must implements interface %s.",
						sConfigurator, IPackConfigurator.class.getName()));
			}
			
			return (IPackConfigurator)configuratorClass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(String.format("Can't initiate pack configurator %s.", sConfigurator), e);
		}
	}

	private CopyLibraryOperation[] getCopyLibraryOperations(Scope packModuleScope, String[] sCopyLibraries) {
		if (sCopyLibraries.length == 0)
			return null;
		
		CopyLibraryOperation[] copyLibraries = new CopyLibraryOperation[sCopyLibraries.length];
		for (int i = 0; i < sCopyLibraries.length; i++) {
			String libraryName = sCopyLibraries[i];
			Scope scope = packModuleScope;
			boolean optional = false;
			int featuresSeparator = sCopyLibraries[i].indexOf(" - ");
			if (featuresSeparator != -1) {
				libraryName = sCopyLibraries[i].substring(0, featuresSeparator).trim();
				String sFeatures = sCopyLibraries[i].substring(featuresSeparator + 3).trim();
				if (sFeatures == null || sFeatures.isEmpty()) {						
					throw new RuntimeException("Illegal pack module configuration format. Check pack_modules.ini file.");
				}
				
				StringTokenizer st = new StringTokenizer(sFeatures, ",");
				while (st.hasMoreTokens()) {
					String feature = st.nextToken();
					if ("system".equals(feature)) {
						scope = Scope.SYSTEM;
					} else if ("optional".equals(feature)) {
						optional = true;
					} else {
						throw new RuntimeException(String.format("Illegal copy operation feature '%s' for library '%s'. " +
								"Check pack_modules.ini file.", feature, libraryName));						
					}
				}
			}
			
			copyLibraries[i] = new CopyLibraryOperation(libraryName, scope, optional);
		}
		
		return copyLibraries;
	}

	private boolean isPackModulesLoaded() {
		return packModulesLoaded == true;
	}

}
