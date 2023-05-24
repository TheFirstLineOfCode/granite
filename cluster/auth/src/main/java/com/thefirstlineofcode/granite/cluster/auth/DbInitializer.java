package com.thefirstlineofcode.granite.cluster.auth;

import org.bson.Document;
import org.pf4j.Extension;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.thefirstlineofcode.granite.framework.adf.mongodb.AbstractDbInitializer;

@Extension
public class DbInitializer extends AbstractDbInitializer {

	@Override
	public void initialize(MongoDatabase database) {
		if (collectionExistsInDb(database, "users"))
			return;
		
		database.createCollection("users");
		MongoCollection<Document> users = database.getCollection("users");
		users.createIndex(Indexes.ascending("name"), new IndexOptions().unique(true));
	}

}
