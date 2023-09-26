package com.thefirstlineofcode.granite.cluster.im;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.thefirstlineofcode.granite.framework.core.adf.data.IIdProvider;
import com.thefirstlineofcode.granite.framework.im.SubscriptionNotification;

@Document(D_SubscriptionNotification.COLLECTION_NAME_SUBSCRIPTION_NOTIFICATIONS)
public class D_SubscriptionNotification extends SubscriptionNotification implements IIdProvider<String> {
	public static final String COLLECTION_NAME_SUBSCRIPTION_NOTIFICATIONS = "subscription_notifications";
	
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
