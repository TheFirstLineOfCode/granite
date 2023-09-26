package com.thefirstlineofcode.granite.framework.adf.mongodb;

public abstract class DataContributorAdapter implements IDataContributor {
	@Override
	public DataObjectMapping<?>[] getDataObjectMappings() {
		Class<?>[] dataObjects = getDataObjects();
		if (dataObjects == null || dataObjects.length == 0)
			return null;
		
		DataObjectMapping<?>[] dataObjectMappings = new DataObjectMapping<?>[dataObjects.length];
		for (int i = 0; i < dataObjects.length; i++) {
			dataObjectMappings[i] = new DataObjectMapping<>(dataObjects[i]);
		}
		
		return dataObjectMappings;
	}
	
	protected Class<?>[] getDataObjects() {
		return null;
	}
}
