package com.thefirstlineofcode.granite.cluster.node.commons.options;

public class BooleanTypeConverter implements TypeConverter<Boolean> {

	@Override
	public Boolean convert(String name, String value) {
		return Boolean.parseBoolean(value);
	}

}
