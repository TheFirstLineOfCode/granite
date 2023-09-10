package com.thefirstlineofcode.granite.cluster.node.mgtnode;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.thefirstlineofcode.granite.cluster.node.commons.utils.NodeUtils;

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
		
		// Set log directory for logback(see logback.xml)
		System.setProperty("mgtnode.logs.dir", options.getHomeDir() + "/logs");
		
		if (options.isLanucherRunMode()) {
			lanuch(args, options);
		} else if (options.isProcessRunMode()) {
			start(options);			
		} else {
			throw new RuntimeException(String.format("Unknown run mode: %s.", options.getRunMode()));
		}
	}

	private static void lanuch(String[] args, Options options) {
		String javaVersion = System.getProperty("java.version");
		if (!javaVersion.startsWith("11.") && !javaVersion.startsWith("17.")) {
			throw new RuntimeException(String.format("Only Java11 && Java17 supported. But your Java version is %s", javaVersion));
		}
		
		List<String> cmdList = new ArrayList<>();
		cmdList.add("java");
		
		NodeUtils.addVmParametersForIgnite(cmdList);
		
		if (options.isDebug()) {
			cmdList.add(String.format("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:%d", options.getDebugPort()));
		}
		
		cmdList.add("-jar");
		URI uri = null;
		try {
			uri = Main.class.getProtectionDomain().getCodeSource().
					getLocation().toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException("Can't get jar name for management node.", e);
		}
		
		String mgtNodeJar = Paths.get(uri).getFileName().toString();
		cmdList.add(mgtNodeJar);
		
		cmdList.add("--run-mode=process");
		
		for (String arg : args) {
			cmdList.add(arg);
		}
		
		String[] cmdArray = new String[cmdList.size()];
		cmdArray = cmdList.toArray(cmdArray);
		try {
			System.out.println("Starting management node in process mode...");
			ProcessBuilder pb = new ProcessBuilder(cmdArray).
					redirectInput(Redirect.INHERIT).
					redirectError(Redirect.INHERIT).
					redirectOutput(Redirect.INHERIT);
			Map<String, String> env = pb.environment();
			for (String key : System.getenv().keySet()) {
				env.put(key, System.getenv(key));
			}
			
			Process process = pb.start();
			process.waitFor();
		} catch (IOException e) {
			throw new RuntimeException("Can't run management node in process.", e);
		} catch (InterruptedException e) {
			throw new RuntimeException("Management node process execution error.", e);
		}
	}

	private static void start(Options options) {
		options.setDeployDir(options.getHomeDir() + "/deploy");
		options.setAppnodeRuntimesDir(options.getDeployDir() + "/runtimes");
		if (options.getRepositoryDir() == null) {
			options.setRepositoryDir(options.getHomeDir() + "/repository");
		}
		
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
