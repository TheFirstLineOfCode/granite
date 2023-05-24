package com.thefirstlineofcode.granite.framework.adf.mybatis;

import java.net.URL;

import org.pf4j.ExtensionPoint;

public interface IDataContributor extends ExtensionPoint {
	TypeHandlerMapping[] getTypeHandlerMappings();
	DataObjectMapping<?>[] getDataObjectMappings();
	URL[] getMappers();
	URL[] getInitScripts();
}
