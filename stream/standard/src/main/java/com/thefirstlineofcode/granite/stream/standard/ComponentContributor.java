package com.thefirstlineofcode.granite.stream.standard;

import org.pf4j.Extension;

import com.thefirstlineofcode.granite.framework.core.repository.IComponentsContributor;

@Extension
public class ComponentContributor implements IComponentsContributor {

	@Override
	public Class<?>[] getComponentClasses() {
		return new Class<?>[] {
			SocketMessageReceiver.class,
			StandardClientMessageProcessor.class
		};
	}

}
