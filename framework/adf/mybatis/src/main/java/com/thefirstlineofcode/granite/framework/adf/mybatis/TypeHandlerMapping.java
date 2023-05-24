package com.thefirstlineofcode.granite.framework.adf.mybatis;

import org.apache.ibatis.type.TypeHandler;

public class TypeHandlerMapping {
	public Class<?> type;
	public Class<?> typeHandlerType;
	
	public <T> TypeHandlerMapping(Class<? extends TypeHandler<T>> typeHandlerType) {
		this((Class<T>)null, (Class<? extends TypeHandler<T>>)typeHandlerType);
	}
	
	public <T> TypeHandlerMapping(Class<T> type, Class<? extends TypeHandler<T>> typeHandlerType) {
		this.type = type;
		this.typeHandlerType = typeHandlerType;
	}
	
}
