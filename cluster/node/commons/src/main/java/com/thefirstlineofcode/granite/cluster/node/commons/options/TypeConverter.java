package com.thefirstlineofcode.granite.cluster.node.commons.options;

public interface TypeConverter<T> {
	T convert(String name, String value);
}