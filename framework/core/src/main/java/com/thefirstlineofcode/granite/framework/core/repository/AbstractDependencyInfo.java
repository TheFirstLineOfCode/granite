package com.thefirstlineofcode.granite.framework.core.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDependencyInfo extends GenericRepositoryWare implements IDependencyInfo {
	private static final Logger logger = LoggerFactory.getLogger(AbstractDependencyInfo.class);
	
	protected String bareId;
	protected int bindedComponentsCount;
	protected List<IComponentInfo> bindedComponents;
	
	public AbstractDependencyInfo(String id, String bareId, Class<?> type, int bindedComponentsCount) {
		super(id, type);
		this.bareId = bareId;
		this.bindedComponentsCount = bindedComponentsCount;
		bindedComponents = new ArrayList<>();
	}

	@Override
	public boolean isAvailable() {
		if (bindedComponentsCount != bindedComponents.size())
			return false;
		
		for (IComponentInfo component : bindedComponents) {
			if (!component.isAvailable())
				return false;
		}
		
		return true;
	}

	@Override
	public void addBindedComponent(IComponentInfo component) {
		if (bindedComponents.contains(component))
			return;
		
		bindedComponents.add(component);
		
		if (bindedComponents.size() > bindedComponentsCount)
			if (logger.isWarnEnabled()) {
				logger.warn("{} needs {} binded components, But {} binded components have injected.",
					new Object[] {id, bindedComponentsCount, bindedComponents.size()});
			}
	}

	@Override
	public List<IComponentInfo> getBindedComponents() {
		return Collections.unmodifiableList(bindedComponents);
	}

	@Override
	public void removeBindedComponent(IComponentInfo component) {
		bindedComponents.remove(component);
	}
	
	public String getBareId() {
		return bareId;
	}

}
