package com.thefirstlineofcode.granite.framework.core;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.granite.framework.core.adf.ApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;
import com.thefirstlineofcode.granite.framework.core.repository.IRepository;
import com.thefirstlineofcode.granite.framework.core.repository.IServiceListener;
import com.thefirstlineofcode.granite.framework.core.repository.IServiceWrapper;
import com.thefirstlineofcode.granite.framework.core.repository.Repository;
import com.thefirstlineofcode.granite.framework.core.repository.ServiceCreationException;

public class Server implements IServer, IServiceListener {
	private static final Logger logger = LoggerFactory.getLogger(Server.class);
		
	protected IServerConfiguration configuration;
	
	protected IApplicationComponentService appComponentService;
	protected IRepository repository;
	protected Map<String, IService> services;
		
	public Server(IServerConfiguration configuration) {
		this.configuration = configuration;
		this.appComponentService = createAppComponentService();
		services = new HashMap<>();
	}
	
	protected IApplicationComponentService createAppComponentService() {
		return new ApplicationComponentService(configuration);
	}

	@Override
	public void start() throws Exception {
		repository = createRepository();
		
		appComponentService.setRepository(repository);
		appComponentService.start();
		
		initRepository();
		
		logger.info("Granite Server has Started");
	}

	protected Repository createRepository() {
		return new Repository(configuration, appComponentService);
	}

	private void initRepository() {
		repository.setServiceListener(this);
		repository.init();
	}

	@Override
	public void stop() throws Exception {
		for (Map.Entry<String, IService> entry : services.entrySet()) {
			try {
				entry.getValue().stop();
			} catch (Exception e) {
				if (logger.isErrorEnabled()) {
					logger.error(String.format("Can't stop service which's ID is '%s'.", entry.getKey()), e);
				}
				
				throw new RuntimeException(String.format("Can't stop service which's ID is '%s' correctly.",
						entry.getKey()), e);
			}
		}
		
		if (appComponentService != null)
			appComponentService.stop();
		
		logger.info("Granite Server has stopped.");
	}

	@Override
	public IServerContext getServerContext() {
		return new ServerContext(this, repository, (ApplicationComponentService)appComponentService);
	}

	@Override
	public void available(IServiceWrapper serviceWrapper) {
		for (String disableService : configuration.getDisabledServices()) {
			if (serviceWrapper.getId().equals(disableService))
				return;
		}
		
		try {
			createAndRunService(serviceWrapper);
		} catch (ServiceCreationException e) {
			if (logger.isErrorEnabled()) {
				logger.error(String.format("Can't create service which's ID is '%s'.", serviceWrapper.getId()), e);
			}
			
			throw new RuntimeException(String.format("Can't create service which's ID is '%s'.",
					serviceWrapper.getId()), e);
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error(String.format("Can't start service which's ID is %s.", serviceWrapper.getId()), e);
			}
			
			throw new RuntimeException(String.format("Can't start service which's ID is '%s'.",
					serviceWrapper.getId()), e);
		}			
	}

	private void createAndRunService(IServiceWrapper serviceWrapper) throws Exception {
		IService service = serviceWrapper.create();
		services.put(serviceWrapper.getId(), service);
		
		service.start();
	}

	@Override
	public IServerConfiguration getConfiguration() {
		return configuration;
	}
}
