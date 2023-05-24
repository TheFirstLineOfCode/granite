package com.thefirstlineofcode.granite.xeps.ibr;

import org.pf4j.Extension;

import com.thefirstlineofcode.granite.framework.core.repository.IComponentsContributor;

@Extension
public class ComponentsContributor implements IComponentsContributor {

	@Override
	public Class<?>[] getComponentClasses() {
		return new Class<?>[] {Registrar.class, IbrSupportedClientMessageProcessor.class};
	}

}
