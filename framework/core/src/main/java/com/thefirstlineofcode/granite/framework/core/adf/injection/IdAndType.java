package com.thefirstlineofcode.granite.framework.core.adf.injection;

public class IdAndType {
	public String id;
	public Class<?> type;
	
	public IdAndType(String id, Class<?> type) {
		this.id = id;
		this.type = type;
	}
}
