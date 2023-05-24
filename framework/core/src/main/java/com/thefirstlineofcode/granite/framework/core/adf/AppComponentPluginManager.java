package com.thefirstlineofcode.granite.framework.core.adf;

import org.pf4j.DefaultPluginFactory;
import org.pf4j.DefaultPluginManager;
import org.pf4j.Plugin;
import org.pf4j.PluginFactory;
import org.pf4j.PluginWrapper;

public class AppComponentPluginManager extends DefaultPluginManager {
	private IApplicationComponentService appComponentService;
	
	public AppComponentPluginManager(IApplicationComponentService appComponentService) {
		this.appComponentService = appComponentService;
	}
	
	@Override
	protected PluginFactory createPluginFactory() {
		return new AppComponentPluginFactory();
	}
	

	
	private class AppComponentPluginFactory extends DefaultPluginFactory {
		@Override
		public Plugin create(PluginWrapper pluginWrapper) {
			Plugin plugin = super.create(pluginWrapper);
			if (plugin == null)
				return null;
			
			return appComponentService.inject(plugin);
		}		
	}
}
