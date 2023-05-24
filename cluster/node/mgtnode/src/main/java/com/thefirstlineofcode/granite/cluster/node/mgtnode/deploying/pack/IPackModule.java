package com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack;

import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlan;

public interface IPackModule {
	public enum Scope {
		SYSTEM,
		PLUGIN
	}
	
	String getName();
	Scope getScope();
	String[] getDependedModules();
	CopyLibraryOperation[] getCopyLibraries();
	IPackConfigurator getCallback();
	
	void copyLibraries(IPackContext context);
	void configure(IPackContext context, DeployPlan configuration);
	
	boolean isLibrariesCopied();
	boolean isConfigured();
}
