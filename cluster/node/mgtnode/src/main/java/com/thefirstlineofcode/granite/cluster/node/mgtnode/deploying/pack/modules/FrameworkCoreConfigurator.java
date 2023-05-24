package com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.modules;

import java.util.Properties;

import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlan;
import com.thefirstlineofcode.granite.cluster.node.commons.deploying.Global;
import com.thefirstlineofcode.granite.cluster.node.commons.utils.StringUtils;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.IPackConfigurator;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.IPackContext;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.config.ConfigFiles;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.config.IConfig;

public class FrameworkCoreConfigurator implements IPackConfigurator {	
	@Override
	public void configure(IPackContext context, DeployPlan configuration) {
		configureServerIni(context);
		configureComponentBindingIni(context);
		configureGlobalFeatureParams(context);
	}

	private void configureGlobalFeatureParams(IPackContext context) {
		IConfig config = context.getConfigManager().createOrGetConfig(
				context.getRuntimeConfigurationDir().toPath(), ConfigFiles.GRANITE_COMPONENTS_CONFIG_FILE);
		Properties globalParams = context.getDeployPlan().getNodeTypes().get(
				context.getNodeType()).getConfiguration("global");
		
		String sessionCallbackCheckInterval = globalParams.getProperty("session-callback-check-interval");
		if (sessionCallbackCheckInterval != null) {
			try {
				Integer.parseInt(sessionCallbackCheckInterval);
			} catch (Exception e) {
				throw new IllegalArgumentException("Global feature parameter 'session-callback-check-interval' must be an integer.");
			}
			
			IConfig sessionManagerConfig = config.getSection("session.manager");
			sessionManagerConfig.addOrUpdateProperty("session.callback.check.interval", sessionCallbackCheckInterval);
		}
	}

	private void configureComponentBindingIni(IPackContext context) {
		// Just create an empty component binding configuration file.
		context.getConfigManager().createOrGetConfig(
				context.getRuntimeConfigurationDir().toPath(), ConfigFiles.GRANITE_COMPONENT_BINDING_CONFIG_FILE);
	}

	private void configureServerIni(IPackContext context) {
		IConfig config = context.getConfigManager().createOrGetConfig(
				context.getRuntimeConfigurationDir().toPath(), ConfigFiles.GRANITE_SERVER_CONFIG_FILE);
		
		config.addComment("Change domain.name and domain.alias.names to your registered doman names.");
		config.addOrUpdateProperty("domain.name", context.getDeployPlan().getCluster().getDomainName());
		
		String[] domainAliasNames = context.getDeployPlan().getCluster().getDomainAliasNames();
		if (domainAliasNames != null && domainAliasNames.length != 0) {
			config.addOrUpdateProperty("domain.alias.names", StringUtils.arrayToString(domainAliasNames));
		}
		
		config.addOrUpdateProperty("component.binding.profile", "${config.dir}/" + ConfigFiles.GRANITE_COMPONENT_BINDING_CONFIG_FILE);
		
		config.addComment("You can uncomment the line below to disable some services.\r\ndisabled.services=stream.service");
		
		String messageFormat = context.getDeployPlan().getGlobal().getMessageFormat();
		if (Global.MESSAGE_FORMAT_BINARY.equals(messageFormat)) {
			config.addOrUpdateProperty("message-format", messageFormat);
		}
	}
}
