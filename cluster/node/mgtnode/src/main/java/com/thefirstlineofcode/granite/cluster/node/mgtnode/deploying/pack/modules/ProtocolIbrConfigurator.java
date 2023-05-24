package com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.modules;

import java.util.Properties;

import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlan;
import com.thefirstlineofcode.granite.cluster.node.commons.deploying.NodeType;
import com.thefirstlineofcode.granite.cluster.node.commons.utils.StringUtils;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.IPackConfigurator;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.IPackContext;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.config.ConfigFiles;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.config.IConfig;

public class ProtocolIbrConfigurator extends AbilityStreamConfigurator implements IPackConfigurator {

	@Override
	public void configure(IPackContext context, DeployPlan configuration) {
		IConfig config = context.getConfigManager().createOrGetConfig(
				context.getRuntimeConfigurationDir().toPath(), ConfigFiles.GRANITE_COMPONENT_BINDING_CONFIG_FILE);
		
		configureIbrSupportedStreamService(context, configuration, config);
		configureParsingService(config);
	}
	
	private void configureIbrSupportedStreamService(IPackContext context, DeployPlan configuration, IConfig config) {
		configureIbrSupportedStreamServiceComponents(config);
		configureStreamFeatureParameters(context, configuration);
	}
	
	protected void configureStreamFeatureParameters(IPackContext context, DeployPlan configuration) {
		IConfig config = context.getConfigManager().createOrGetConfig(
				context.getRuntimeConfigurationDir().toPath(), ConfigFiles.GRANITE_COMPONENTS_CONFIG_FILE);		
		NodeType nodeType = configuration.getNodeTypes().get(context.getNodeType());
		Properties properties = nodeType.getConfiguration("ability-stream");
	
		configureSocketAddressAndConnectionTimeout(config.getSection("socket.message.receiver"), properties);
		configureTlsAndSasl(config.getSection("ibr.supported.client.message.processor"), properties);
	}
	
	@Override
	protected void configureTlsAndSasl(IConfig config, Properties properties) {
		Boolean tlsRequired = null;
		String[] saslSupportedMechanisms = null;
		if (properties.getProperty("tls.required") != null) {
			tlsRequired = Boolean.parseBoolean(properties.getProperty("tls.required"));
		}
		
		if (properties.getProperty("sasl.supported.mechanisms") != null) {
			saslSupportedMechanisms = StringUtils.stringToArray(properties.getProperty("sasl.supported.mechanisms"));
			
			for (String mechanism : saslSupportedMechanisms) {
				if (!"DIGEST-MD5".equals(mechanism) &&
						!"CRAM-MD5".equals(mechanism)) {
					throw new IllegalArgumentException(String.format("Unsupported SASL mechanism '%s'. Parameter 'sasl.supported.mechanisms' of 'ability-stream' is incorrect.", mechanism));
				}
			}
		}
		
		if (tlsRequired != null)
			config.addOrUpdateProperty("stream.service$socket.message.receiver$ibr.client.message.processor$tls.required", Boolean.toString(tlsRequired));
		
		if (saslSupportedMechanisms != null)
			config.addOrUpdateProperty("stream.service$socket.message.receiver$ibr.client.message.processor$sasl.supported.mechanisms", StringUtils.arrayToString(saslSupportedMechanisms));
	}
	
	private void configureIbrSupportedStreamServiceComponents(IConfig config) {
		config.addOrUpdateProperty("stream.service$delivery.message.receiver", "cluster.routing.2.stream.message.receiver");
		config.addOrUpdateProperty("cluster.routing.2.stream.message.receiver$ignite", "cluster.ignite");
		config.addOrUpdateProperty("cluster.routing.2.stream.message.receiver$local.node.id.provider", "cluster.local.node.id.provider");
		config.addOrUpdateProperty("cluster.routing.2.stream.message.receiver$message.processor", "default.delivery.message.processor");
		config.addOrUpdateProperty("stream.service$client.message.receivers", "socket.message.receiver");
		config.addOrUpdateProperty("socket.message.receiver$session.manager", "cluster.session.manager");
		config.addOrUpdateProperty("socket.message.receiver$message.processor", "ibr.supported.client.message.processor");
		config.addOrUpdateProperty("socket.message.receiver$router", "cluster.router");
		config.addOrUpdateProperty("socket.message.receiver$ignite", "cluster.ignite");
		config.addOrUpdateProperty("socket.message.receiver$local.node.id.provider", "cluster.local.node.id.provider");
		config.addOrUpdateProperty("cluster.local.node.id.provider$ignite", "cluster.ignite");
		config.addOrUpdateProperty("ibr.supported.client.message.processor$authenticator", "cluster.authenticator");
		config.addOrUpdateProperty("ibr.supported.client.message.processor$session.manager", "cluster.session.manager");
		config.addOrUpdateProperty("ibr.supported.client.message.processor$message.channel", "cluster.stream.2.parsing.message.channel");
		config.addOrUpdateProperty("cluster.stream.2.parsing.message.channel$connector", "cluster.stream.2.parsing.message.receiver");
		config.addOrUpdateProperty("ibr.supported.client.message.processor$router", "cluster.router");
		config.addOrUpdateProperty("cluster.router$ignite", "cluster.ignite");
		config.addOrUpdateProperty("cluster.router$session.manager", "cluster.session.manager");
		config.addOrUpdateProperty("ibr.supported.client.message.processor$registrar", "default.registrar");
		config.addOrUpdateProperty("default.registrar$account.manager", "cluster.account.manager");
		config.addOrUpdateProperty("default.registrar$registration.strategy", "cluster.registration.strategy");
	}
	
}
