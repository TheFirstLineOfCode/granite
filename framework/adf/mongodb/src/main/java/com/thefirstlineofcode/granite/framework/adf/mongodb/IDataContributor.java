package com.thefirstlineofcode.granite.framework.adf.mongodb;

import org.pf4j.ExtensionPoint;

public interface IDataContributor extends ExtensionPoint {
	DataObjectMapping<?>[] getDataObjectMappings();
}
