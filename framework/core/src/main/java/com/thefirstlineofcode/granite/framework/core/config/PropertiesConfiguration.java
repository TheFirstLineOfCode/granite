package com.thefirstlineofcode.granite.framework.core.config;

import java.util.Properties;

public class PropertiesConfiguration implements IConfiguration {
	protected Properties properties;
	
	public PropertiesConfiguration(Properties properties) {
		this.properties = properties;
	}

	@Override
	public Boolean getBoolean(String key) {
		return getBoolean(key, null);
	}

	@Override
	public Boolean getBoolean(String key, Boolean defaultValue) {
		String value = properties.getProperty(key);
		if (value == null)
			return defaultValue;
		
		return Boolean.parseBoolean(value);
	}

	@Override
	public Integer getInteger(String key) {
		return getInteger(key, null);
	}

	@Override
	public Integer getInteger(String key, Integer defaultValue) {
		String value = properties.getProperty(key);
		if (value == null)
			return defaultValue;
		
		return Integer.parseInt(value);
	}

	@Override
	public String getString(String key) {
		return getString(key, null);
	}

	@Override
	public String getString(String key, String defaultValue) {
		String value = properties.getProperty(key);
		if (value == null)
			return defaultValue;
		
		return value;
	}

	@Override
	public String[] keys() {
		Object[] objects = properties.keySet().toArray();
		String[] keys = new String[objects.length];
		
		for (int i = 0; i < objects.length; i++) {
			keys[i] = (String)objects[i];
		}
		
		return keys;
	}
	
	public Properties getProperties() {
		return properties;
	}
}
