package com.thefirstlineofcode.granite.framework.core.repository;

import com.thefirstlineofcode.granite.framework.core.IService;

public class ServiceWrapper implements IServiceWrapper {
	private IComponentInfo componentInfo;
	private IRepository repository;
	
	public ServiceWrapper(IRepository repository, IComponentInfo componentInfo) {
		this.repository = repository;
		
		if (!componentInfo.isService()) {
			throw new IllegalArgumentException(String.format("Component(id: %s) should be a service.",
					componentInfo.getId()));
		}
		
		this.componentInfo = componentInfo;
	}

	@Override
	public String getId() {
		return componentInfo.getId();
	}

	@Override
	public IService create() throws ServiceCreationException {
		try {
			return (IService)createComponent(componentInfo);
		} catch (Exception e) {
			throw new ServiceCreationException(String.format("Can't create service '%s'.",
					componentInfo.getId()), e);
		}
	}

	private Object createComponent(IComponentInfo componentInfo) throws Exception {
		synchronized (repository) {
			return repository.get(componentInfo.getId());
		}
	}
}
