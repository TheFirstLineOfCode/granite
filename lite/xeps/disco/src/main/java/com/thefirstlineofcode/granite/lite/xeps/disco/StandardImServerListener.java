package com.thefirstlineofcode.granite.lite.xeps.disco;

import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;

import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentServiceAware;
import com.thefirstlineofcode.granite.framework.core.annotations.AppComponent;

@AppComponent("standard.im.server.listener")
public class StandardImServerListener implements IApplicationComponentServiceAware {
	private boolean standardImServer = false;
	private boolean standardStream = false;
	
	public boolean isIMServer() {
		return standardImServer;
	}
	
	public boolean isStandardStream() {
		return standardStream;
	}

	@Override
	public void setApplicationComponentService(IApplicationComponentService appComponentService) {
		PluginManager pluginManager = appComponentService.getPluginManager();
		PluginWrapper imPlugin = pluginManager.getPlugin("granite-im");
		if (imPlugin != null && imPlugin.getPluginState() == PluginState.STARTED)
			standardImServer = true;
		
		PluginWrapper standardStreamPlugin = pluginManager.getPlugin("granite-stream-standard");
		if (standardStreamPlugin != null && standardStreamPlugin.getPluginState() == PluginState.STARTED)
			standardStream = true;
	}
}
