package com.thefirstlineofcode.granite.lite.xeps.msgoffline;

import com.thefirstlineofcode.granite.framework.core.adf.data.IIdProvider;
import com.thefirstlineofcode.granite.framework.im.OfflineMessage;

public class D_OfflineMessage extends OfflineMessage implements IIdProvider<String> {
	private String id;
	
	@Override
	public void setId(String id) {
		this.id = id;
	}
	
	@Override
	public String getId() {
		return id;
	}
	
}
