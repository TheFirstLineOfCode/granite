package com.thefirstlineofcode.granite.framework.core.pipeline;

import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.session.ISessionManager;

public abstract class AbstractMessageReceiver implements IMessageReceiver {
	
	protected IMessageProcessor messageProcessor;
	protected boolean active = false;
	
	protected IMessageChannel messageChannel;
	protected ISessionManager sessionManager;
	
	@Override
	public synchronized boolean isActive() {
		return active;
	}
	
	@Override
	public synchronized void start() throws Exception {
		if (active)
			return;
		
		doStart();
		active = true;
	}
	
	@Override
	public synchronized void stop() throws Exception {
		if (!active)
			return;
		
		doStop();
		active = false;
	}
	
	protected abstract void doStart() throws Exception ;
	protected abstract void doStop() throws Exception;
	
	@Override
	@Dependency("message.processor")
	public void setMessageProcessor(IMessageProcessor messageProcessor) {
		this.messageProcessor = messageProcessor;
	}
	
	@Dependency("session.manager")
	public void setSessionManager(ISessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}
	
	@Dependency("message.channel")
	public void setMessageChannel(IMessageChannel messageChannel) {
		this.messageChannel = messageChannel;
	}

}
