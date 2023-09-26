package com.thefirstlineofcode.granite.cluster.im;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.thefirstlineofcode.granite.framework.core.adf.data.IIdProvider;
import com.thefirstlineofcode.granite.framework.im.Subscription;

@Document(D_Subscription.COLLECTION_NAME_SUBSCRIPTIONS)
public class D_Subscription extends Subscription implements IIdProvider<String> {
	public static final String COLLECTION_NAME_SUBSCRIPTIONS = "subscriptions";
	
	@Id
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
