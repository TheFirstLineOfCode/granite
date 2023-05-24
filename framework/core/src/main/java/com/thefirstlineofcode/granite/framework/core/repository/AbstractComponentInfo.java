package com.thefirstlineofcode.granite.framework.core.repository;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractComponentInfo extends GenericRepositoryWare implements IComponentInfo {
	protected List<IDependencyInfo> dependencies;
	protected boolean singleton;
	protected volatile Object instance;
	private Object singletonLock = new Object();

	public AbstractComponentInfo(String id, Class<?> type, boolean singleton) {
		super(id, type);
		
		dependencies = new ArrayList<>();
		this.singleton = singleton;
	}
	
	@Override
	public void addDependency(IDependencyInfo dependency) {
		dependencies.add(dependency);
	}
	
	@Override
	public void removeDependency(IDependencyInfo dependency) {
		dependencies.remove(dependency);
	}
	
	@Override
	public IDependencyInfo[] getDependencies() {
		return dependencies.toArray(new IDependencyInfo[dependencies.size()]);
	}
	
	@Override
	public boolean isAvailable() {
		for (IDependencyInfo dependency : dependencies) {
			if (!dependency.isAvailable())
				return false;
		}
		
		return true;
	}
	
	@Override
	public boolean isSingleton() {
		return singleton;
	}
	
	@Override
	public Object create() throws CreationException {
		if (!singleton) {
			return doCreate();
		}
		
		synchronized (singletonLock) {
			if (instance == null)
				instance = doCreate();
			
			return instance;
		}
	}
	
	@Override
	public Class<?> getType() {
		return super.getType();
	}
	
	protected abstract Object doCreate() throws CreationException;

}