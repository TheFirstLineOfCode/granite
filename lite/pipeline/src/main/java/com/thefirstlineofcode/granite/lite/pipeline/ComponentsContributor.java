package com.thefirstlineofcode.granite.lite.pipeline;

import org.pf4j.Extension;

import com.thefirstlineofcode.granite.framework.core.repository.IComponentsContributor;

@Extension
public class ComponentsContributor implements IComponentsContributor {

	@Override
	public Class<?>[] getComponentClasses() {
		return new Class<?>[] {
			LocalNodeIdProvider.class,
			MessageChannel.class,
			MessageReceiver.class,
			Router.class,
			Routing2StreamMessageReceiver.class
		};
	}

}
