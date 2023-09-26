package com.thefirstlineofcode.granite.cluster.nodes.commons.options;

public interface TypeConverter<T> {
	T convert(String name, String value);
}