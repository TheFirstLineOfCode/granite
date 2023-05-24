package com.thefirstlineofcode.granite.framework.core.repository;

import java.util.List;

public interface IDependencyInfo {
	String getId();
	String getBareId();
	boolean isAvailable();
	void addBindedComponent(IComponentInfo component);
	void removeBindedComponent(IComponentInfo component);
	List<IComponentInfo> getBindedComponents();
	void injectDependency(Object object, Object dependency);
	IDependencyInfo getAliasDependency(String alias, int bindedComponentsCount);
}
