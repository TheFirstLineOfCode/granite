package com.thefirstlineofcode.granite.framework.core.repository;

import org.pf4j.ExtensionPoint;

public interface IComponentsContributor extends ExtensionPoint {
	Class<?>[] getComponentClasses();
}
