package com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.modules;

import java.util.Properties;

import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlan;
import com.thefirstlineofcode.granite.cluster.node.commons.deploying.NodeType;
import com.thefirstlineofcode.granite.cluster.node.commons.utils.StringUtils;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.IPackConfigurator;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.IPackContext;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.config.ConfigFiles;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.config.IConfig;

public class AbilityStreamConfigurator implements IPackConfigurator {

	@Override
	public void configure(IPackContext context, DeployPlan configuration) {
		// If protocol IBR exists. We configure stream in ProtocolIbrConfigurator.
		if (context.getPackModule("protocol-ibr") != null) {
			return;
		}
		
		IConfig config = context.getConfigManager().createOrGetConfig(context.getRuntimeConfigurationDir().toPath(),
				ConfigFiles.GRANITE_COMPONENT_BINDING_CONFIG_FILE);
		
		configureStandardStreamService(context, configuration, config);
		configureParsingService(config);
	}

	protected void configureParsingService(IConfig config) {
		config.addOrUpdateProperty("parsing.service$parsing.message.receiver", "cluster.stream.2.parsing.message.receiver");
		config.addOrUpdateProperty("cluster.stream.2.parsing.message.receiver$session.manager", "cluster.session.manager");
		config.addOrUpdateProperty("cluster.stream.2.parsing.message.receiver$message.channel", "cluster.parsing.2.processing.message.channel");
		config.addOrUpdateProperty("cluster.parsing.2.processing.message.channel$connector", "cluster.parsing.2.processing.message.receiver");
		config.addOrUpdateProperty("cluster.stream.2.parsing.message.receiver$message.processor", "default.message.parsing.processor");
		config.addOrUpdateProperty("cluster.session.manager$ignite", "cluster.ignite");
		config.addOrUpdateProperty("cluster.parsing.2.processing.message.channel$node.runtime.configuration", "cluster.node.runtime.configuration");
	}

	private void configureStandardStreamService(IPackContext context, DeployPlan configuration, IConfig config) {
		configureStandardStreamServiceComponents(config);
		configureStreamFeatureParameters(context, configuration);
	}

	protected void configureStreamFeatureParameters(IPackContext context, DeployPlan configuration) {
		IConfig config = context.getConfigManager().createOrGetConfig(
				context.getRuntimeConfigurationDir().toPath(), ConfigFiles.GRANITE_COMPONENTS_CONFIG_FILE);		
		NodeType nodeType = configuration.getNodeTypes().get(context.getNodeType());
		Properties properties = nodeType.getConfiguration("ability-stream");
	
		configureSocketAddressAndConnectionTimeout(config.getSection("socket.message.receiver"), properties);
		configureTlsAndSasl(config.getSection("standard.client.message.processor"), properties);
	}

	protected void configureTlsAndSasl(IConfig config, Properties properties) {
		Boolean tlsRequired = null;
		String[] saslSupportedMechanisms = null;
		if (properties.getProperty("tls-required") != null) {
			tlsRequired = Boolean.parseBoolean(properties.getProperty("tls-required"));
		}
		
		if (properties.getProperty("sasl-supported-mechanisms") != null) {
			saslSupportedMechanisms = StringUtils.stringToArray(properties.getProperty("sasl-supported-mechanisms"));
			
			for (String mechanism : saslSupportedMechanisms) {
				if (!"PLAIN".equals(mechanism) &&
						!"DIGEST-MD5".equals(mechanism) &&
						!"CRAM-MD5".equals(mechanism)) {
					throw new IllegalArgumentException(String.format("Unsupported SASL mechanism '%s'. Parameter 'sasl.supported.mechanisms' of 'ability-stream' is incorrect.", mechanism));
				}
			}
		}
		
		if (tlsRequired != null)
			config.addOrUpdateProperty("tls.required", Boolean.toString(tlsRequired));
		
		if (saslSupportedMechanisms != null)
			config.addOrUpdateProperty("sasl.supported.mechanisms", StringUtils.arrayToString(saslSupportedMechanisms));
	}

	protected void configureSocketAddressAndConnectionTimeout(IConfig config, Properties properties) {
		String ip = properties.getProperty("ip");
		if (ip != null)
			config.addOrUpdateProperty("ip", ip);
		
		String port;
		try {
			port = properties.getProperty("port");
			if (port != null) {
				Integer.parseInt(port);
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Parameter 'port' of 'ability-stream' feature must be an integer.");
		}
		if (port != null)
			config.addOrUpdateProperty("port", port);
		
		String connectionTimeout;
		try {
			connectionTimeout = properties.getProperty("connection-timeout", "12000");
			if (connectionTimeout != null) {
				Integer.parseInt(connectionTimeout);
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Parameter 'connection-timeout' of 'ability-stream' feature must be an integer.");
		}
		if (connectionTimeout != null)
			config.addOrUpdateProperty("connection.timeout", connectionTimeout);
	}

	private void configureStandardStreamServiceComponents(IConfig config) {
		config.addOrUpdateProperty("stream.service$client.message.receivers", "socket.message.receiver");
		config.addOrUpdateProperty("socket.message.receiver$session.manager", "cluster.session.manager");
		config.addOrUpdateProperty("socket.message.receiver$message.processor", "standard.client.message.processor");
		config.addOrUpdateProperty("socket.message.receiver$router", "cluster.router");
		config.addOrUpdateProperty("socket.message.receiver$ignite", "cluster.ignite");
		config.addOrUpdateProperty("socket.message.receiver$local.node.id.provider", "cluster.local.node.id.provider");
		config.addOrUpdateProperty("cluster.local.node.id.provider$ignite", "cluster.ignite");
		config.addOrUpdateProperty("standard.client.message.processor$authenticator", "cluster.authenticator");
		config.addOrUpdateProperty("standard.client.message.processor$session.manager", "cluster.session.manager");
		config.addOrUpdateProperty("standard.client.message.processor$message.channel", "cluster.stream.2.parsing.message.channel");
		config.addOrUpdateProperty("cluster.stream.2.parsing.message.channel$connector", "cluster.stream.2.parsing.message.receiver");
		config.addOrUpdateProperty("standard.client.message.processor$router", "cluster.router");
		config.addOrUpdateProperty("cluster.router$ignite", "cluster.ignite");
		config.addOrUpdateProperty("cluster.router$session.manager", "cluster.session.manager");
		
		config.addOrUpdateProperty("stream.service$delivery.message.receiver", "cluster.routing.2.stream.message.receiver");
		config.addOrUpdateProperty("cluster.routing.2.stream.message.receiver$ignite", "cluster.ignite");
		config.addOrUpdateProperty("cluster.routing.2.stream.message.receiver$local.node.id.provider", "cluster.local.node.id.provider");
		config.addOrUpdateProperty("cluster.routing.2.stream.message.receiver$message.processor", "default.delivery.message.processor");
	}

}
