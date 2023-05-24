package com.thefirstlineofcode.granite.framework.core.adf;

import org.pf4j.ExtensionPoint;

public interface IAppComponentsContributor extends ExtensionPoint {
	Class<?>[] getAppComponentClasses();
}
