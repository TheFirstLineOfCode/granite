package com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack;

import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlan;

public class PackModule implements IPackModule {
	private String name;
	private Scope scope;
	private String[] dependedModules;
	private CopyLibraryOperation[] copyLibraries;
	private IPackConfigurator configurator;
	private boolean librariesCopied;
	private boolean configured;
	
	public PackModule(String name, Scope scope, String[] dependedModules, CopyLibraryOperation[] copyLibraries,
			IPackConfigurator configurator) {
		this.name = name;
		this.scope = scope;
		this.dependedModules = dependedModules;
		this.copyLibraries = copyLibraries;
		this.configurator = configurator;
		
		librariesCopied = false;
		configured = false;
	}
	
	@Override
	public void copyLibraries(IPackContext context) {
		if (dependedModules != null) {
			for (String sModule : dependedModules) {
				IPackModule module = context.getPackModule(sModule);
				if (!module.isLibrariesCopied()) {
					module.copyLibraries(context);
				}
			}
		}
		
		if (copyLibraries != null) {
			for (CopyLibraryOperation operation : copyLibraries) {
				operation.copy(context);
			}
		}
		
		librariesCopied = true;
	}
	
	@Override
	public void configure(IPackContext context, DeployPlan configuration) {
		if (dependedModules != null) {
			for (String sModule : dependedModules) {
				IPackModule module = context.getPackModule(sModule);
				if (!module.isConfigured()) {
					module.configure(context, configuration);
				}
			}
		}
		
		if (configurator != null) {
			configurator.configure(context, configuration);
		}
		
		configured = true;
	}
	
	@Override
	public boolean isLibrariesCopied() {
		return librariesCopied;
	}
	
	@Override
	public boolean isConfigured() {
		return configured;
	}
	
	@Override
	public String[] getDependedModules() {
		return dependedModules;
	}
	
	@Override
	public CopyLibraryOperation[] getCopyLibraries() {
		return copyLibraries;
	}
	
	@Override
	public IPackConfigurator getCallback() {
		return configurator;
	}

	@Override
	public Scope getScope() {
		return scope;
	}

	@Override
	public String getName() {
		return name;
	}
}
