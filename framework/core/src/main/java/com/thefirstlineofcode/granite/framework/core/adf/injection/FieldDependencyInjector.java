package com.thefirstlineofcode.granite.framework.core.adf.injection;

import java.lang.reflect.Field;

public class FieldDependencyInjector extends AbstractDependencyInjector {
	private Field field;
	
	public FieldDependencyInjector(Field field, IDependencyFetcher fetcher) {
		super(fetcher);
		this.field = field;
	}
	
	@Override
	protected void doInject(Object object, Object dependency) {
		boolean oldAccessible = field.canAccess(object);
		field.setAccessible(true);
		
		try {
			field.set(object, dependency);
		} catch (Exception e) {
			throw new RuntimeException("Can't inject dependency by field.", e);
		} finally {
			field.setAccessible(oldAccessible);
		}
	}

}
