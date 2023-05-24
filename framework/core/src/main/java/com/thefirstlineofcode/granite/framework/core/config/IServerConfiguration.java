package com.thefirstlineofcode.granite.framework.core.config;

public interface IServerConfiguration {
	public static final String SERVER_CONFIG_KEY_DISABLED_SERVICES = "disabled.services";
	public static final String SERVER_CONFIG_KEY_DOMAIN_NAME = "domain.name";
	public static final String SERVER_CONFIG_KEY_DOMAIN_ALIAS_NAMES = "domain.alias.names";
	public static final String SERVER_CONFIG_KEY_MESSAGE_FORMAT = "message.format";
	public static final String SERVER_CONFIG_KEY_COMPONENT_BINDING_PROFILE = "component.binding.profile";
	public static final String SERVER_CONFIG_KEY_APPLICATION_LOG_NAMESPACES = "application.log.namespaces";
	public static final String SERVER_CONFIG_KEY_CUSTOMIZED_LIBRARIES = "customized.libraries";
	public static final String SERVER_CONFIG_KEY_LOG_LEVEL = "log.level";
	public static final String SERVER_CONFIG_KEY_ENABLE_THIRDPARTY_LOGS = "enable.thirdparty.logs";	
	public static final String SERVER_CONFIG_KEY_HSQL_PORT = "hsql.port";	
	
	String[] getDisabledServices();
	String getServerHome();
	String getSystemLibsDir();
	String getPluginsDir();
	String getConfigurationDir();
	String getLogsDir();
	String getDomainName();
	String[] getDomainAliasNames();
	String getMessageFormat();
	String getComponentBindingProfile();
	String[] getApplicationLogNamespaces();
	String[] getCustomizedLibraries();
	String getLogLevel();
	boolean isThirdpartyLogEnabled();
	int getHSqlPort();
}
