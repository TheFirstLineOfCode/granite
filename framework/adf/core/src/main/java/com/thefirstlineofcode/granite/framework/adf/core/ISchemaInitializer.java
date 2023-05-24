package com.thefirstlineofcode.granite.framework.adf.core;

import org.pf4j.ExtensionPoint;

public interface ISchemaInitializer extends ExtensionPoint {
	String getInitialScript();
}
