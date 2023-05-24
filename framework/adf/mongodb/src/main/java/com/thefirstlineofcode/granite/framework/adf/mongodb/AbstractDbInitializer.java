package com.thefirstlineofcode.granite.framework.adf.mongodb;

import com.mongodb.client.MongoDatabase;

public abstract class AbstractDbInitializer implements IDbInitializer {

	protected boolean collectionExistsInDb(MongoDatabase database, String collectionName) {
		for (String aCollectionName : database.listCollectionNames()) {
			if (aCollectionName.equals(collectionName))
				return true;
		}
		
		return false;
	}
}
