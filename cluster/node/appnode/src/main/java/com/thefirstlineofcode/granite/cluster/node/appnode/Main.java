package com.thefirstlineofcode.granite.cluster.node.appnode;

public class Main {
	public static void main(String[] args) {
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
		
		if (options.getRuntimesDir() == null) {
			options.setRuntimesDir(options.getHomeDir() + "/runtimes");
		}
		
		// set log directory for logback(see logback.xml)
		System.setProperty("appnode.log.dir", options.getHomeDir() + "/log");
		
		try {
			new Starter().start(options);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.out.println("There was someting wrong. Application terminated.");
		}
	}
}
