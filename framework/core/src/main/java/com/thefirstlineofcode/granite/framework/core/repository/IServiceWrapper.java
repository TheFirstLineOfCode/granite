package com.thefirstlineofcode.granite.framework.core.repository;

import com.thefirstlineofcode.granite.framework.core.IService;

public interface IServiceWrapper {
	String getId();
	IService create() throws ServiceCreationException;
}
