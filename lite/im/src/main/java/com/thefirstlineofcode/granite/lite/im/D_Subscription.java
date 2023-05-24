package com.thefirstlineofcode.granite.lite.im;

import com.thefirstlineofcode.granite.framework.core.adf.data.IIdProvider;

public class D_Subscription extends com.thefirstlineofcode.granite.framework.im.Subscription implements IIdProvider<String> {
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
