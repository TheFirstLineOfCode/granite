package com.thefirstlineofcode.granite.framework.adf.mongodb;

import org.pf4j.ExtensionPoint;

import com.mongodb.client.MongoDatabase;

public interface IDbInitializer extends ExtensionPoint {
	void initialize(MongoDatabase database);
}
