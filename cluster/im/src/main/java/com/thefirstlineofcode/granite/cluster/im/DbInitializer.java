package com.thefirstlineofcode.granite.cluster.im;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.thefirstlineofcode.granite.framework.adf.mongodb.AbstractDbInitializer;

public class DbInitializer extends AbstractDbInitializer {

	private static final String FIELD_NAME_CONTACT = "contact";
	private static final String FIELD_NAME_USER = "user";

	@Override
	public void initialize(MongoDatabase database) {
		if (collectionExistsInDb(database, D_Subscription.COLLECTION_NAME_SUBSCRIPTIONS))
			return;
		
		database.createCollection(D_Subscription.COLLECTION_NAME_SUBSCRIPTIONS);
		MongoCollection<Document> subscriptions = database.getCollection(D_Subscription.COLLECTION_NAME_SUBSCRIPTIONS);
		subscriptions.createIndex(Indexes.compoundIndex(Indexes.ascending(FIELD_NAME_USER), Indexes.ascending(FIELD_NAME_CONTACT)), new IndexOptions().unique(true));
		
		database.createCollection(D_SubscriptionNotification.COLLECTION_NAME_SUBSCRIPTION_NOTIFICATIONS);
		MongoCollection<Document> subscriptionNofications = database.getCollection(D_SubscriptionNotification.COLLECTION_NAME_SUBSCRIPTION_NOTIFICATIONS);
		subscriptionNofications.createIndex(Indexes.ascending(FIELD_NAME_USER));
	}

}
