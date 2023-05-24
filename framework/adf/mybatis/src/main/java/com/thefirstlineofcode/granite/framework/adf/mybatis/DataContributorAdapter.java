package com.thefirstlineofcode.granite.framework.adf.mybatis;

import java.net.URL;

public abstract class DataContributorAdapter implements IDataContributor {
	private static final String DIRECTORY_OF_MAPPER_RESOURCES = "META-INF/data/";
	private static final String DIRECTORY_OF_INIT_SCRIPT_RESOURCES = "META-INF/data/";

	@Override
	public TypeHandlerMapping[] getTypeHandlerMappings() {
		return null;
	}
	
	@Override
	public URL[] getMappers() {
		String[] mapperFileNames = getMapperFileNames();
		if (mapperFileNames == null || mapperFileNames.length == 0)
			return null;;
		
		URL[] urls = new URL[mapperFileNames.length];
		for (int i = 0; i < mapperFileNames.length; i++) {
			String resourceName = DIRECTORY_OF_MAPPER_RESOURCES + mapperFileNames[i];
			urls[i] = getClass().getClassLoader().getResource(resourceName);
		}
		
		return urls;
	}

	protected String[] getMapperFileNames() {
		return null;
	}
	
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

	@Override
	public URL[] getInitScripts() {
		String[] initScriptFileNames = getInitScriptFileNames();
		if (initScriptFileNames == null || initScriptFileNames.length == 0)
			return null;;
		
		URL[] urls = new URL[initScriptFileNames.length];
		for (int i = 0; i < initScriptFileNames.length; i++) {
			String resourceName = DIRECTORY_OF_INIT_SCRIPT_RESOURCES + initScriptFileNames[i];
			urls[i] = getClass().getClassLoader().getResource(resourceName);
		}
		
		return urls;
	}

	protected String[] getInitScriptFileNames() {
		return null;
	}
}
