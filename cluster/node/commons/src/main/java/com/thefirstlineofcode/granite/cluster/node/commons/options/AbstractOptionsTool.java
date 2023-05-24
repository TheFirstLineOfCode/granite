package com.thefirstlineofcode.granite.cluster.node.commons.options;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public abstract class AbstractOptionsTool<T extends OptionsBase> {
	private static final String OPTION_NAME_CONFIGURATION_DIR = "configuration-dir";
	
	private Map<String, OptionRule> optionRules;
	private String configFileName;
	
	public AbstractOptionsTool(String configFileName) {
		this.configFileName = configFileName;
		
		optionRules = new HashMap<>();
		optionRules = buildOptionRules(optionRules);
		
		optionRules.put(
				OPTION_NAME_CONFIGURATION_DIR,
				new OptionRule().
					setRange(OptionRule.Range.COMMAND_LINE).
					setDataType(OptionRule.DataType.STRING)
		);
	}

	protected abstract Map<String, OptionRule> buildOptionRules(Map<String, OptionRule> optionRules);
	protected abstract T createOptions();
	protected abstract void printUsage();
	
	public T parseOptions(String[] args) {
		if (args.length == 1 && args[0].equals("--help")) {
			T options = createOptions();
			options.setHelp(true);
			
			return options;
		}
		
		Map<String, String> commandLineOptions = new HashMap<>();
		for (int i = 0; i < args.length; i++) {
			if (!args[i].startsWith("--")) {
				throw new IllegalArgumentException("Illegal option format.");
			}
			
			int equalSignIndex = args[i].indexOf('=');
			if (equalSignIndex == 2 ||
					equalSignIndex == (args[i].length() - 1)) {
				throw new IllegalArgumentException("Illegal option format.");
			}
			
			String name, value;
			if (equalSignIndex == -1) {
				name = args[i].substring(2,  args[i].length());
				value = "TRUE";
			} else {
				name = args[i].substring(2, equalSignIndex);
				value = args[i].substring(equalSignIndex + 1, args[i].length());
			}
			
			if (name.equals("help")) {
				throw new IllegalArgumentException("Illegal option format.");
			}
			
			commandLineOptions.put(name, value);
		}
		
		String homeDirPath = getHomeDir();
		
		String configurationDirPath = (String)commandLineOptions.get(OPTION_NAME_CONFIGURATION_DIR);
		if (configurationDirPath == null) {
			configurationDirPath = homeDirPath + "/configuration";
			
			if (!new File(configurationDirPath).exists()) {
				configurationDirPath = new File(homeDirPath).getParent() + "/configuration";
			}
		}
		commandLineOptions.put(OPTION_NAME_CONFIGURATION_DIR, configurationDirPath);
		
		File configurationDir = new File(configurationDirPath);
		if (!configurationDir.exists() || !configurationDir.isDirectory()) {
			throw new IllegalArgumentException(String.format("Illegal configruation directory. '%s' doesn't exist or isn't a directory.", configurationDirPath));
		}
		
		T options = readAndMergeConfigFile(commandLineOptions, new File(configurationDir, configFileName));
		
		options.setHomeDir(homeDirPath);
		
		return options;
	}
	
	protected String getHomeDir() {
		URL classPathRoot = this.getClass().getResource("/");
		
		if (classPathRoot == null) {
			URL metaInfo = this.getClass().getResource("/META-INF");
			int colonIndex =  metaInfo.getFile().indexOf('!');
			String jarPath =  metaInfo.getPath().substring(0, colonIndex);
			
			int lastSlashIndex = jarPath.lastIndexOf('/');
			String jarParentDirPath = jarPath.substring(6, lastSlashIndex);
			
			return jarParentDirPath;
		} else {
			int targetIndex = classPathRoot.getPath().lastIndexOf("/target");	
			return classPathRoot.getPath().substring(0, targetIndex + 7);
		}
	}

	private T readAndMergeConfigFile(Map<String, String> commandLineOptions, File configFile) {
		if (!configFile.exists() || !configFile.isFile()) {
			throw new IllegalArgumentException(String.format("Configuration file '%s' doesn't exist or it isn't a file.", configFile.getPath()));
		}
		
		Properties config = new Properties();
		try {
			config.load(configFile.toURI().toURL().openStream());
		} catch (Exception e) {
			throw new IllegalArgumentException("Can't read configuration file.");
		}
		
		T options = createOptions();
		for (Map.Entry<Object, Object> entry : config.entrySet()) {
			String name = (String)entry.getKey();
			String value = (String)entry.getValue();
			
			OptionRule rule = optionRules.get(name);
			if (rule == null || rule.getRange() == OptionRule.Range.COMMAND_LINE) {
				throw new IllegalArgumentException(String.format("Illegal configuration item '%s' from configuration file %s.",
						name, configFile));
			}
			
			rule.getOptionSetter().setOption(options, name, replaceReferences(value));
		}
		
		List<String> usedOptions = new ArrayList<>();
		for (Entry<String, String> option : commandLineOptions.entrySet()) {
			String name = option.getKey();
			String value = option.getValue();
			
			OptionRule rule = optionRules.get(name);
			if (rule == null || rule.getRange() == OptionRule.Range.CONFIG_FILE) {
				throw new IllegalArgumentException(String.format("Illegal option: %s.", name));
			}
			
			if (usedOptions.contains(name)) {
				throw new IllegalArgumentException(String.format("Reduplicate option: %s.", name));
			}
			
			rule.getOptionSetter().setOption(options, name, value);
			usedOptions.add(name);
		}
		
		return options;
	}

	private String replaceReferences(String value) {
		// TODO replace references of value
		return value;
	}
}