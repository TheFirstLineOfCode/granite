package com.thefirstlineofcode.granite.lite.xeps.disco;

import org.pf4j.Extension;

import com.thefirstlineofcode.granite.framework.core.adf.IAppComponentsContributor;

@Extension
public class AppComponentsContributor implements IAppComponentsContributor {

	@Override
	public Class<?>[] getAppComponentClasses() {
		return new Class<?>[] {
			DiscoProcessor.class,
			StandardImServerListener.class
		};
	}

}
