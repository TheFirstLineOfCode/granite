package com.thefirstlineofcode.granite.framework.core.adf.injection;

import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentService;

public class AppComponentFetcher implements IDependencyFetcher {
	private IApplicationComponentService appComponentService;
	private IdAndType idAndType;
	
	public AppComponentFetcher(IApplicationComponentService appComponentService,
			IdAndType idAndType) {
		this.appComponentService = appComponentService;
		this.idAndType = idAndType;
	}
	
	@Override
	public Object fetch() {
		Object component = appComponentService.getAppComponent(idAndType.id, idAndType.type);
		if (component == null)
			throw new IllegalArgumentException(String.format("No component which's component id is %s in repository.",
					idAndType.id));
		
		return component;
	}

}
