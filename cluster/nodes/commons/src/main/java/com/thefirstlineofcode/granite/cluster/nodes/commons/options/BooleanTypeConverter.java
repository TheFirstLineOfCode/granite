package com.thefirstlineofcode.granite.cluster.nodes.commons.options;

public class BooleanTypeConverter implements TypeConverter<Boolean> {

	@Override
	public Boolean convert(String name, String value) {
		return Boolean.parseBoolean(value);
	}

}
