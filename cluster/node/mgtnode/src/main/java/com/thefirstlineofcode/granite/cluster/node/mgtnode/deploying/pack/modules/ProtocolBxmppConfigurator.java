package com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.modules;

import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlan;
import com.thefirstlineofcode.granite.cluster.node.commons.deploying.Global;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.IPackConfigurator;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.IPackContext;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.config.ConfigFiles;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.config.IConfig;

public class ProtocolBxmppConfigurator implements IPackConfigurator  {

	@Override
	public void configure(IPackContext context, DeployPlan configuration) {
		IConfig config = context.getConfigManager().createOrGetConfig(
				context.getRuntimeConfigurationDir().toPath(), ConfigFiles.GRANITE_SERVER_CONFIG_FILE);
		config.addOrUpdateProperty("message.format", Global.MESSAGE_FORMAT_BINARY);
	}
	
}
