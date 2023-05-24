package com.thefirstlineofcode.granite.framework.core.repository;

import com.thefirstlineofcode.granite.framework.core.IService;

public class GenericComponentInfo extends AbstractComponentInfo implements IComponentInfo {
	
	public GenericComponentInfo(String id, Class<?> type) {
		super(id, type, true);
	}
	
	public boolean isService() {
		return IService.class.isAssignableFrom(type);
	}
	
	public String toString() {
		if (isService()) {
			return String.format("Service[%s, %s]", id, type);
		} else {
			return String.format("Component[%s, %s]", id, type);
		}
	}

	@Override
	public Object doCreate() throws CreationException {
		try {
			return getType().getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new CreationException("Can't create component", e);
		}
	}

	@Override
	public IComponentInfo getAliasComponent(String alias) {
		return new GenericComponentInfo(alias, type);
	}
}
