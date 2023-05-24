package com.thefirstlineofcode.granite.framework.core.config;

public class DummyConfiguration implements IConfiguration {

	@Override
	public Boolean getBoolean(String key) {
		return getBoolean(key, null);
	}

	@Override
	public Boolean getBoolean(String key, Boolean defaultValue) {
		return defaultValue;
	}

	@Override
	public Integer getInteger(String key) {
		return getInteger(key, null);
	}

	@Override
	public Integer getInteger(String key, Integer defaultValue) {
		return defaultValue;
	}

	@Override
	public String getString(String key) {
		return getString(key, null);
	}

	@Override
	public String getString(String key, String defaultValue) {
		return defaultValue;
	}

	@Override
	public String[] keys() {
		return new String[0];
	}

}
