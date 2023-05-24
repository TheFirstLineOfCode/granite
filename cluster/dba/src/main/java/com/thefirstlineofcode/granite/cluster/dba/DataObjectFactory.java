package com.thefirstlineofcode.granite.cluster.dba;

import com.thefirstlineofcode.granite.framework.core.adf.data.IDataObjectFactory;
import com.thefirstlineofcode.granite.framework.core.annotations.AppComponent;

@AppComponent("data.object.factory")
public class DataObjectFactory implements IDataObjectFactory {	
	@SuppressWarnings("unchecked")
	@Override
	public <K, V extends K> V create(Class<K> clazz) {
		try {
			return (V)clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(String.format("Can't create data object for class '%s'.", clazz.getName()), e);
		}
	}
}
