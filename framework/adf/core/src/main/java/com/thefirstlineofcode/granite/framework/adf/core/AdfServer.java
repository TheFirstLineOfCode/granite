package com.thefirstlineofcode.granite.framework.adf.core;

import com.thefirstlineofcode.granite.framework.core.Server;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;

public class AdfServer extends Server {

	public AdfServer(IServerConfiguration configuration) {
		super(configuration);
	}
	
	protected IApplicationComponentService createAppComponentService() {
		return new AdfComponentService(configuration);
	}

}
