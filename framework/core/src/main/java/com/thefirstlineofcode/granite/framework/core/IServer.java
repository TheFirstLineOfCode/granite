package com.thefirstlineofcode.granite.framework.core;

import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;

public interface IServer {
	void start() throws Exception;
	void stop() throws Exception;
	
	IServerConfiguration getConfiguration();
	IServerContext getServerContext();
}
