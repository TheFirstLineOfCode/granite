package com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.modules;

import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlan;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.IPackConfigurator;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.IPackContext;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.config.ConfigFiles;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.config.IConfig;

public class AbilityProcessingConfigurator implements IPackConfigurator {

	@Override
	public void configure(IPackContext context, DeployPlan deployPlan) {
		IConfig config = context.getConfigManager().createOrGetConfig(context.getRuntimeConfigurationDir().toPath(),
				ConfigFiles.GRANITE_COMPONENT_BINDING_CONFIG_FILE);
		configureProcessingService(config);
		configureRoutingService(config);
	}

	private void configureProcessingService(IConfig config) {
		config.addOrUpdateProperty("processing.service$processing.message.receiver", "cluster.parsing.2.processing.message.receiver");
		config.addOrUpdateProperty("cluster.parsing.2.processing.message.receiver$node.runtime.configuration", "cluster.node.runtime.configuration");
		config.addOrUpdateProperty("cluster.parsing.2.processing.message.receiver$session.manager", "cluster.session.manager");
		config.addOrUpdateProperty("cluster.parsing.2.processing.message.receiver$message.channel", "cluster.any.2.routing.message.channel");
		config.addOrUpdateProperty("cluster.any.2.routing.message.channel$connector", "cluster.any.2.routing.message.receiver");
		config.addOrUpdateProperty("cluster.parsing.2.processing.message.receiver$message.processor", "default.protocol.processing.processor");
		config.addOrUpdateProperty("default.protocol.processing.processor$authenticator", "cluster.authenticator");
	}

	private void configureRoutingService(IConfig config) {
		config.addPropertyIfAbsent("routing.service$routing.message.receiver", "cluster.any.2.routing.message.receiver");
		config.addPropertyIfAbsent("cluster.any.2.routing.message.receiver$session.manager", "cluster.session.manager");
		config.addPropertyIfAbsent("cluster.any.2.routing.message.receiver$message.channel", "cluster.routing.2.stream.message.channel");
		config.addPropertyIfAbsent("cluster.any.2.routing.message.receiver$message.processor", "default.routing.processor");
		config.addPropertyIfAbsent("cluster.routing.2.stream.message.channel$node.runtime.configuration", "cluster.node.runtime.configuration");
		config.addPropertyIfAbsent("cluster.routing.2.stream.message.channel$router", "cluster.router");
		config.addPropertyIfAbsent("cluster.router$session.manager", "cluster.session.manager");
		config.addPropertyIfAbsent("cluster.any.2.routing.message.receiver$message.processor", "default.routing.processor");
	}

}
