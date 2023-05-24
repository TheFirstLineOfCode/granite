package com.thefirstlineofcode.granite.cluster.node.commons.options;

public class IntTypeConverter implements TypeConverter<Integer> {

	@Override
	public Integer convert(String name, String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(String.format("Illegal option format. option '%s' must be a number", name));
		}
	}
}