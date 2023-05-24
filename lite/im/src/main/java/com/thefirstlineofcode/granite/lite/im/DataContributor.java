package com.thefirstlineofcode.granite.lite.im;

import org.pf4j.Extension;

import com.thefirstlineofcode.granite.framework.adf.mybatis.DataContributorAdapter;

@Extension
public class DataContributor extends DataContributorAdapter {

	@Override
	public Class<?>[] getDataObjects() {
		return new Class<?>[] {
			D_Subscription.class,
			D_SubscriptionNotification.class
		};
	}
	
	@Override
	protected String[] getInitScriptFileNames() {
		return new String[] {
			"im.sql"	
		};
	}
	
	@Override
	protected String[] getMapperFileNames() {
		return new String[] {
			"SubscriptionMapper.xml"
		};
	}

}
