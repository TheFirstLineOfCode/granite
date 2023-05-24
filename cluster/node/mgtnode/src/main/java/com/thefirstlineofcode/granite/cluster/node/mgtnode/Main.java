package com.thefirstlineofcode.granite.cluster.node.mgtnode;

public class Main {
	public static void main(String[] args) throws Exception {
		OptionsTool optionTool = new OptionsTool();
		
		Options options = null;
		try {
			options = optionTool.parseOptions(args);
		} catch (IllegalArgumentException e) {
			if (e.getMessage() != null) {
				System.out.println(String.format("Unable to parse options. %s", e.getMessage()));
			} else {
				System.out.println("Unable to parse options.");
			}
			
			optionTool.printUsage();
			
			return;
		}
		
		if (options.isHelp()) {
			optionTool.printUsage();
			return;
		}
		
		options.setDeployDir(options.getHomeDir() + "/deploy");
		options.setAppnodeRuntimesDir(options.getDeployDir() + "/runtimes");
		if (options.getRepositoryDir() == null) {
			options.setRepositoryDir(options.getHomeDir() + "/repository");
		}
		
		// Set log directory for logback(see logback.xml)
		System.setProperty("mgtnode.log.dir", options.getHomeDir() + "/log");
		
		try {
			boolean started = new Starter().start(options);
			if (!started) {
				System.err.println("There was something wrong. Application terminated.");
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.err.println("There was something wrong. Application terminated.");
		}
		
	}
}
