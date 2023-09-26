package com.thefirstlineofcode.granite.cluster.auth;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.thefirstlineofcode.granite.framework.core.adf.data.IIdProvider;
import com.thefirstlineofcode.granite.framework.core.auth.Account;

@Document(D_Account.COLLECTION_NAME_ACCOUNTS)
public class D_Account extends Account implements IIdProvider<String> {
	public static final String COLLECTION_NAME_ACCOUNTS = "accounts";
	
	@Id
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
