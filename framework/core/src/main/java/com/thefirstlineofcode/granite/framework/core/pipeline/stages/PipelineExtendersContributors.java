package com.thefirstlineofcode.granite.framework.core.pipeline.stages;

import java.util.List;

import org.pf4j.PluginManager;

public class PipelineExtendersContributors {
	private static volatile PipelineExtendersContributors instance;
	
	private PluginManager pluginManager;
	private List<IPipelineExtendersContributor> contributors;
	
	private PipelineExtendersContributors(PluginManager pluginManager) {
		this.pluginManager = pluginManager;
	}
	
	public static PipelineExtendersContributors getInstance(PluginManager pluginManager) {
		if (instance != null)
			return instance;
		
		synchronized (PipelineExtendersContributors.class) {
			if (instance != null)
				return instance;
			
			instance = new PipelineExtendersContributors(pluginManager);
			return instance;
		}
	}
	
	public IPipelineExtendersContributor[] getContributors() {
		if (contributors == null) {
			contributors = pluginManager.getExtensions(IPipelineExtendersContributor.class);			
		}
		
		if (contributors == null || contributors.size() == 0)
			return new IPipelineExtendersContributor[0];
		
		return contributors.toArray(new IPipelineExtendersContributor[contributors.size()]);
	}
}
