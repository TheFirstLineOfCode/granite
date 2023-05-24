package com.thefirstlineofcode.granite.framework.core.adf.data;

public interface IIdProvider<T> {
	void setId(T id);
	T getId();
}
