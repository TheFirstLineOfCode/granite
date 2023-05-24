package com.thefirstlineofcode.granite.framework.core.adf.injection;

public abstract class AbstractDependencyInjector implements IDependencyInjector {
	protected IDependencyFetcher fetcher;
	
	public AbstractDependencyInjector(IDependencyFetcher fetcher) {
		this.fetcher = fetcher;
	}
	
	@Override
	public void inject(Object object) {
		Object dependency = fetcher.fetch();
		if (dependency == null)
			throw new RuntimeException(String.format("Can't fetch a dependency to inject."));
		
		doInject(object, dependency);
	}
	
	protected abstract void doInject(Object object, Object dependency);
}
