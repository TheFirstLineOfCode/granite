package com.thefirstlineofcode.granite.framework.core.repository;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class ListFieldDependencyInfo extends AbstractDependencyInfo {
	private Field field;

	public ListFieldDependencyInfo(String id, String bareId, Class<?> type, Field field, int bindedComponentsCount) {
		super(id, bareId, null, bindedComponentsCount);
		this.field = field;
	}
	
	@Override
	public String toString() {
		return String.format("List Field Dependency[%s, %s, %s]", id, type, field);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void injectDependency(Object object, Object dependency) {
		boolean accessible = field.canAccess(object);
		try {
			field.setAccessible(true);
			Object list = field.get(object);
			
			if (list == null) {
				list = new ArrayList();
				field.set(object, list);
			}
			
			Method addMethod = list.getClass().getMethod("add", new Class<?>[] {Object.class});
			
			addMethod.invoke(list, dependency);
		} catch (Exception e) {
			throw new RuntimeException(String.format("Can't inject component %s to %s.", dependency, object), e);
		} finally {
			field.setAccessible(accessible);
		}
	}

	@Override
	public IDependencyInfo getAliasDependency(String alias, int bindedComponentsCount) {
		return new ListFieldDependencyInfo(Repository.getFullDependencyId(alias, bareId), bareId, type,
				field, bindedComponentsCount);
	}
}
