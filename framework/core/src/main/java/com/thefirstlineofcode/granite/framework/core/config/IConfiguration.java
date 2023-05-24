package com.thefirstlineofcode.granite.framework.core.config;

public interface IConfiguration {	
	Boolean getBoolean(String key);
	Boolean getBoolean(String key, Boolean defaultValue);
	Integer getInteger(String key);
	Integer getInteger(String key, Integer defaultValue);
	String getString(String key);
	String getString(String key, String defaultValue);
	
	String[] keys();
}
