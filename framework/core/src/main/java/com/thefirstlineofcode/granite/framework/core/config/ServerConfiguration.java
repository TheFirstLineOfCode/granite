package com.thefirstlineofcode.granite.framework.core.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;
import java.util.StringTokenizer;

import com.thefirstlineofcode.basalt.xmpp.Constants;


public class ServerConfiguration implements IServerConfiguration {
	private static final String DIRECTORY_NAME_CONFIGURATION = "configuration";
	private static final String DIRECTORY_NAME_LOGS = "logs";
	private static final String DIRECTORY_NAME_LIBS = "libs";
	private static final String DIRECTORY_NAME_PLUGINS = "plugins";
	private static final String NAME_SERVER_CONFIG_FILE = "server.ini";
	
	private String domainName = "localhost";
	private String[] domainAliasNames = new String[0];
	private String messageFormat = Constants.MESSAGE_FORMAT_XML;

	private String[] disabledServices = new String[0];
	private String componentBindingProfile = "${config.dir}/component-binding.ini";
	
	private String serverHome;
	private String configDir;
	private String logsDir;
	
	private String[] applicationLogNamespaces;
	private String[] customizedLibraries;
	
	private String logLevel;
	private boolean enableThirdpartyLogs = false;
	
	private int hSqlPort = 9001;
	
	public ServerConfiguration(String serverHome) {
		this.serverHome = serverHome;
		readConfiguratioin(getServerConfigFile(getConfigurationDir()));
	}
	
	private File getServerConfigFile(String configDir) {
		File serverConfigFile = new File(configDir, NAME_SERVER_CONFIG_FILE);
		if (serverConfigFile.exists() && serverConfigFile.isFile())
			return serverConfigFile;
		
		return null;
	}

	@Override
	public String[] getDisabledServices() {
		return disabledServices;
	}

	@Override
	public String getServerHome() {
		return serverHome;
	}

	private void readConfiguratioin(File configFile) {
		if (configFile == null)
			return;

		Properties properties = new Properties();
		Reader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(configFile));
			properties.load(reader);
			
			for (Object key : properties.keySet()) {
				String sKey = (String)key;
				if (SERVER_CONFIG_KEY_DOMAIN_NAME.equals(key)) {
					domainName = properties.getProperty(sKey);
				} else if (SERVER_CONFIG_KEY_DOMAIN_ALIAS_NAMES.equals(key)) {
					setDomainAliasNames(properties.getProperty(sKey));
				} else if (SERVER_CONFIG_KEY_MESSAGE_FORMAT.equals(key)) {
					setMessageFormat(properties.getProperty(sKey));
				} else if (SERVER_CONFIG_KEY_DISABLED_SERVICES.equals(key)) {
					setDisabledServices(properties.getProperty(sKey));
				} else if (SERVER_CONFIG_KEY_APPLICATION_LOG_NAMESPACES.equals(key)) {
					setApplicationLogNamespaces(properties.getProperty(sKey));
				} else if (SERVER_CONFIG_KEY_COMPONENT_BINDING_PROFILE.equals(key)) {
					setComponentBindingProfile(properties.getProperty(sKey));
				} else if (SERVER_CONFIG_KEY_CUSTOMIZED_LIBRARIES.equals(key)) {
					setCustomizedSystemLibraries(properties.getProperty(sKey));
				} else if (SERVER_CONFIG_KEY_LOG_LEVEL.equals(key)) {
					setLogLevel(properties.getProperty(sKey));
				} else if (SERVER_CONFIG_KEY_ENABLE_THIRDPARTY_LOGS.equals((String)key)) {
					String sEnableThirdPartyLogs = properties.getProperty(sKey);
					if (Boolean.valueOf(sEnableThirdPartyLogs))
						enableThirdpartyLogs();
				} else if (SERVER_CONFIG_KEY_HSQL_PORT.equals(key)) {
					setHSqlPort(properties.getProperty(sKey));
				} else {
					// ignore
					System.out.println(String.format("Unknown server configuration item: '%s'. Ignore it.", key));
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Illegal server configuration. Please check your server configuration file.", e);
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	private void setHSqlPort(String sPort) {
		try {
			hSqlPort = Integer.parseInt(sPort);			
		} catch (NumberFormatException e) {
			throw new RuntimeException("HSql port must be an integer.");
		}
	}

	private void setMessageFormat(String messageFormat) {
		if (Constants.MESSAGE_FORMAT_BINARY.equals(messageFormat) || Constants.MESSAGE_FORMAT_XML.equals(messageFormat)) {
			this.messageFormat = messageFormat;
		} else {
			// ignore
			System.out.println(String.format("Unknown message format: '%s'. Continue to use 'xml' message format.", messageFormat));
		}
	}
	
	@Override
	public String getMessageFormat() {
		return messageFormat;
	}

	private void setApplicationLogNamespaces(String sApplicationLogNamespaces) {
		if (sApplicationLogNamespaces == null || "".equals(sApplicationLogNamespaces))
			return;
		
		StringTokenizer tokenizer = new StringTokenizer(sApplicationLogNamespaces, ",");
		applicationLogNamespaces = new String[tokenizer.countTokens()];
		int i = 0;
		while (tokenizer.hasMoreTokens()) {
			applicationLogNamespaces[i] = tokenizer.nextToken().trim();
			i++;
		}
	}

	private String replacePathVariables(String value) {
		value = value.replace("${config.dir}", getConfigurationDir());
		value = value.replace("${user.home}", System.getProperty("user.home"));
		
		return value;
	}

	private void setDisabledServices(String sDisabledServices) {
		StringTokenizer tokenizer = new StringTokenizer(sDisabledServices, ",");
		disabledServices = new String[tokenizer.countTokens()];
		int i = 0;
		while (tokenizer.hasMoreTokens()) {
			disabledServices[i] = tokenizer.nextToken().trim();
			i++;
		}
	}
	
	private void setDomainAliasNames(String sDomainAliasNames) {
		StringTokenizer tokenizer = new StringTokenizer(sDomainAliasNames, ",");
		domainAliasNames = new String[tokenizer.countTokens()];
		int i = 0;
		while (tokenizer.hasMoreTokens()) {
			domainAliasNames[i] = tokenizer.nextToken().trim();
			i++;
		}
	}
	
	@Override
	public String getDomainName() {
		return domainName;
	}

	@Override
	public String getComponentBindingProfile() {
		return replacePathVariables(componentBindingProfile);
	}
	
	private void setComponentBindingProfile(String componentBindingProfile) {
		this.componentBindingProfile = componentBindingProfile;
	}
	
	@Override
	public String getConfigurationDir() {
		if (configDir == null)
			configDir = new File(getServerHome(), DIRECTORY_NAME_CONFIGURATION).getAbsolutePath();
		
		return configDir;
	}

	@Override
	public String[] getApplicationLogNamespaces() {
		return applicationLogNamespaces;
	}

	@Override
	public String[] getDomainAliasNames() {
		if (domainAliasNames == null) {
			domainAliasNames = new String[0];
		}
		
		return domainAliasNames;
	}

	@Override
	public String getSystemLibsDir() {
		return new File(getServerHome(), DIRECTORY_NAME_LIBS).getAbsolutePath();
	}

	@Override
	public String getPluginsDir() {
		return new File(getServerHome(), DIRECTORY_NAME_PLUGINS).getAbsolutePath();
	}
	
	private void setCustomizedSystemLibraries(String sCustomizedLibraries) {
		if (sCustomizedLibraries == null || "".equals(sCustomizedLibraries))
			return;
		
		StringTokenizer tokenizer = new StringTokenizer(sCustomizedLibraries, ",");
		this.customizedLibraries = new String[tokenizer.countTokens()];
		int i = 0;
		while (tokenizer.hasMoreTokens()) {
			this.customizedLibraries[i] = tokenizer.nextToken().trim();
			i++;
		}		
	}
	
	@Override
	public String[] getCustomizedLibraries() {
		return customizedLibraries;
	}
	
	private void setLogLevel(String logLevel) {
		if (!"debug".equals(logLevel) && !"trace".equals(logLevel) && !"info".equals(logLevel))
			throw new IllegalArgumentException(String.format("Unknown log level %s. Supported option is: 'info', 'debug', 'trace'.", logLevel));
		
		this.logLevel = logLevel;
	}

	@Override
	public String getLogLevel() {
		return logLevel == null ? "info" : logLevel;
	}
	
	private void enableThirdpartyLogs() {
		enableThirdpartyLogs = true;
	}

	@Override
	public boolean isThirdpartyLogEnabled() {
		return enableThirdpartyLogs;
	}

	@Override
	public int getHSqlPort() {
		return hSqlPort;
	}

	@Override
	public String getLogsDir() {
		if (logsDir == null)
			logsDir = new File(getConfigurationDir(), DIRECTORY_NAME_LOGS).getAbsolutePath();
		
		return logsDir;
	}
	
}
