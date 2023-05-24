package com.thefirstlineofcode.granite.framework.core.pipeline;

import com.thefirstlineofcode.granite.framework.core.connection.IConnectionManager;

public interface IMessageReceiver extends IConnectionManager {
	void start() throws Exception;
	void stop() throws Exception;
	
	boolean isActive();
	
	void setMessageProcessor(IMessageProcessor messageProcessor);
}
