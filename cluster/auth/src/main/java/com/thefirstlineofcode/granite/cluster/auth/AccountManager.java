package com.thefirstlineofcode.granite.cluster.auth;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.thefirstlineofcode.granite.framework.core.auth.Account;
import com.thefirstlineofcode.granite.framework.core.auth.IAccountManager;

@Component
public class AccountManager implements IAccountManager {
	@Autowired
	private MongoDatabase database;
	
	@Override
	public void add(Account account) {
		Document doc = new Document().
				append("name", account.getName()).
				append("password", account.getPassword());
		getUsersCollection().insertOne(doc);
	}

	@Override
	public void remove(String name) {
		getUsersCollection().deleteOne(Filters.eq("name", name));
	}

	@Override
	public boolean exists(String name) {
		return getUsersCollection().countDocuments(Filters.eq("name", name)) == 1;
	}

	@Override
	public Account get(String name) {
		MongoCollection<Document> users = getUsersCollection();
		Document doc = users.find(Filters.eq("name", name)).first();
		if (doc == null)
			return null;
		
		D_Account account = new D_Account();
		account.setId(doc.getObjectId("_id").toHexString());
		account.setName(doc.getString("name"));
		account.setPassword(doc.getString("password"));
		
		return account;
	}
	
	private MongoCollection<Document> getUsersCollection() {
		return database.getCollection("users");
	}

}
