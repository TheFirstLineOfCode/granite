package com.thefirstlineofcode.granite.framework.core.adf.data;

public interface IDataObjectFactory {
	public static final String COMPONENT_ID_DATA_OBJECT_FACTORY = "data.object.factory";
	
	<K, V extends K> V create(Class<K> clazz);
}
