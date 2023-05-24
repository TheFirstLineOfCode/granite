package com.thefirstlineofcode.granite.framework.core.repository;

import java.lang.reflect.Method;

public class MethodDependencyInfo extends AbstractDependencyInfo {
	private Method method;
	
	public MethodDependencyInfo(String id, String bareId, Class<?> type, Method method, int bindedComponentsCount) {
		super(id, bareId, type, bindedComponentsCount);
		
		this.method = method;
	}
	
	@Override
	public String toString() {
		return String.format("Method Dependency[%s, %s, %s, %d]", id, type, method, bindedComponentsCount);
	}

	@Override
	public void injectDependency(Object object, Object dependency) {
		try {
			method.invoke(object, new Object[] {dependency});
		} catch (Exception e) {
			throw new RuntimeException(String.format("Can't inject component %s to %s.", dependency, object), e);
		}
	}

	@Override
	public IDependencyInfo getAliasDependency(String alias, int bindedComponentsCount) {
		return new MethodDependencyInfo(Repository.getFullDependencyId(alias, bareId), bareId, type,
				method, bindedComponentsCount);
	}
}
