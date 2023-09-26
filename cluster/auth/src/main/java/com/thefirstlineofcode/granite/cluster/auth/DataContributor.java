package com.thefirstlineofcode.granite.cluster.auth;

import org.pf4j.Extension;

import com.thefirstlineofcode.granite.framework.adf.mongodb.DataContributorAdapter;

@Extension
public class DataContributor extends DataContributorAdapter {
	@Override
	public Class<?>[] getDataObjects() {
		return new Class<?>[] {
			D_Account.class
		};
	}
}
