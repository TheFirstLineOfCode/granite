package com.thefirstlineofcode.granite.framework.core.repository;

import java.lang.reflect.Field;

public class FieldDependencyInfo extends AbstractDependencyInfo {
	private Field field;
	
	public FieldDependencyInfo(String id, String bareId, Class<?> type, Field field, boolean notNull) {
		super(id, bareId, type, notNull ? 1 : 0);
		
		this.field = field;
	}
	
	@Override
	public String toString() {
		return String.format("Field Dependency[%s, %s, %s]", id, type, field);
	}

	@Override
	public void injectDependency(Object object, Object dependency) {
		boolean accessible = field.canAccess(object);
		
		try {
			field.setAccessible(true);
			field.set(object, dependency);
		} catch (Exception e) {
			throw new RuntimeException(String.format("Can't inject component %s to %s.", dependency, object), e);
		} finally {
			field.setAccessible(accessible);
		}
	}

	@Override
	public IDependencyInfo getAliasDependency(String alias, int bindedComponentsCount) {
		return new FieldDependencyInfo(Repository.getFullDependencyId(alias, bareId), bareId, type,
				field, bindedComponentsCount == 1 ? true : false);
	}
}
