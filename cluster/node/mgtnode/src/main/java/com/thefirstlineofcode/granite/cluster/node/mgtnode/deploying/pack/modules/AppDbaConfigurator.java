package com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.modules;

import java.util.List;

import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DbAddress;
import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlan;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.IPackConfigurator;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.IPackContext;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.config.IConfig;

public class AppDbaConfigurator implements IPackConfigurator {

	private static final String FILE_NAME_DB_INI = "db.ini";

	@Override
	public void configure(IPackContext context, DeployPlan deployPlan) {
		IConfig dbConfig = context.getConfigManager().createOrGetConfig(
				context.getRuntimeConfigurationDir().toPath(), FILE_NAME_DB_INI);
		dbConfig.addOrUpdateProperty("addresses", getDbAddressesString(deployPlan.getDb().getAddresses()));
		dbConfig.addOrUpdateProperty("db.name", deployPlan.getDb().getDbName());
		dbConfig.addOrUpdateProperty("user.name", deployPlan.getDb().getUserName());
		dbConfig.addOrUpdateProperty("password", new String(deployPlan.getDb().getPassword()));
	}

	private String getDbAddressesString(List<DbAddress> addresses) {
		StringBuilder sb = new StringBuilder();
		
		for (DbAddress address : addresses) {
			sb.append(address.getHost()).append(':').append(address.getPort()).append(',');
		}
		
		if (sb.length() > 0) {
			sb.delete(sb.length() - 1, sb.length());
		}
		
		return sb.toString();
	}

}
