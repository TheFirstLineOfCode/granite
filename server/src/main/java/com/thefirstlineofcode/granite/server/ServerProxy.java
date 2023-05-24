package com.thefirstlineofcode.granite.server;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.granite.framework.adf.core.AdfComponentService;
import com.thefirstlineofcode.granite.framework.adf.core.AdfServer;
import com.thefirstlineofcode.granite.framework.core.IServer;
import com.thefirstlineofcode.granite.framework.core.Server;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;

public class ServerProxy {
	private static final Logger logger = LoggerFactory.getLogger(ServerProxy.class);
	
	private IServer server;
	
	public IServer start(IServerConfiguration serverConfiguration) {
		server = createServer(serverConfiguration);
		try {
			server.start();
		} catch (Exception e) {
			if (server != null) {
				try {
					server.stop();
				} catch (Exception exception) {
					throw new RuntimeException("Can't stop server correctly.", exception);
				}
			}
			
			logger.error("Can't to start Granite Server correctly.", e);
			throw new RuntimeException("Can't to start Granite Server Correctly.", e);
		}
		
		return server;
	}

	private Server createServer(IServerConfiguration serverConfiguration) {
		return new AdfServer(serverConfiguration) {
			@Override
			protected IApplicationComponentService createAppComponentService() {
				return new AdfComponentService(configuration);
			}
		};
	}
	
	public void stop() {
		try {
			server.stop();
		} catch (Exception e) {
			logger.error("Can't to stop Granite Server correctly.", e);
		}
	}
}
