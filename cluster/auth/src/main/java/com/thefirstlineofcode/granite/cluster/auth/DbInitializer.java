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
	private static final String FIELD_NAME_NAME = "name";

	@Override
	public void initialize(MongoDatabase database) {
		if (collectionExistsInDb(database, D_Account.COLLECTION_NAME_ACCOUNTS))
			return;
		
		database.createCollection(D_Account.COLLECTION_NAME_ACCOUNTS);
		MongoCollection<Document> accounts = database.getCollection(D_Account.COLLECTION_NAME_ACCOUNTS);
		accounts.createIndex(Indexes.ascending(FIELD_NAME_NAME), new IndexOptions().unique(true));
	}

}
