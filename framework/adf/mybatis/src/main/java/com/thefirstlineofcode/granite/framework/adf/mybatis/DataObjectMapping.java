package com.thefirstlineofcode.granite.framework.adf.mybatis;

public class DataObjectMapping<T> {
	public Class<T> domainType;
	public Class<? extends T> dataType;
	
	public DataObjectMapping(Class<? extends T> dataType) {
		this(null, dataType);
	}

	public DataObjectMapping(Class<T> domainType, Class<? extends T> dataType) {
		this.domainType = domainType;
		this.dataType = dataType;
	}
}
