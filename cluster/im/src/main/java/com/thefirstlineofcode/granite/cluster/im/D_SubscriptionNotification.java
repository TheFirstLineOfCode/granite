package com.thefirstlineofcode.granite.cluster.im;

import com.thefirstlineofcode.granite.framework.core.adf.data.IIdProvider;
import com.thefirstlineofcode.granite.framework.im.SubscriptionNotification;

public class D_SubscriptionNotification extends SubscriptionNotification implements IIdProvider<String> {
	private String id;
	
	@Override
	public String getId() {
		return id;
	}
	
	@Override
	public void setId(String id) {
		this.id = id;
	}
	
}
