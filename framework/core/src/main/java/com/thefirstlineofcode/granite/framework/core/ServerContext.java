package com.thefirstlineofcode.granite.framework.core;

import com.thefirstlineofcode.granite.framework.core.adf.ApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.adf.IPluginConfigurations;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;
import com.thefirstlineofcode.granite.framework.core.repository.IRepository;

public class ServerContext implements IServerContext {
	private IServer server;
	private IRepository repository;
	private ApplicationComponentService appComponentService;
	
	public ServerContext(IServer server, IRepository repository,
			ApplicationComponentService appComponentService) {
		this.server = server;
		this.repository = repository;
		this.appComponentService = appComponentService;
	}
	
	@Override
	public IServer getServer() {
		return server;
	}
	
	@Override
	public IServerConfiguration getServerConfiguration() {
		return server.getConfiguration();
	}
	
	@Override
	public IRepository getRepository() {
		return repository;
	}
	
	@Override
	public IApplicationComponentService getApplicationComponentService() {
		return appComponentService;
	}

	@Override
	public IPluginConfigurations getApplicationComponentConfigurations() {
		return appComponentService.getPluginConfigurations();
	}
}
