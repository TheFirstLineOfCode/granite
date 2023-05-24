package com.thefirstlineofcode.granite.cluster.auth;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.thefirstlineofcode.granite.framework.core.auth.IAuthenticator;
import com.thefirstlineofcode.granite.framework.core.auth.PrincipalNotFoundException;

@Component
public class Authenticator implements IAuthenticator {
	@Autowired
	private MongoDatabase database;
	
	@Override
	public Object getCredentials(Object principal) throws PrincipalNotFoundException {
		MongoCollection<Document> users = getUsersCollection();
		Document doc = users.find(Filters.eq("name", (String)principal)).projection(
				new Document("password", 1)).first();
		
		return doc == null ? null : doc.get("password");
	}

	@Override
	public boolean exists(Object principal) {
		return getUsersCollection().countDocuments(Filters.eq("name", (String)principal)) == 1;
	}
	
	private MongoCollection<Document> getUsersCollection() {
		return database.getCollection("users");
	}

}
