package com.thefirstlineofcode.granite.server;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.granite.framework.core.IServer;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.ServerConfiguration;
import com.thefirstlineofcode.granite.framework.core.console.ConsoleSystem;
import com.thefirstlineofcode.granite.framework.core.log.LogFilter;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class Main {
	public static void main(String[] args) {
		new Main().run(args);
	}
	
	private void run(String[] args) {
		Options options = null;
		try {
			options = parseOptions(args);
		} catch (IllegalArgumentException e) {
			if (e.getMessage() != null)
				System.out.println("Error: " + e.getMessage());
			printUsage();
			return;
		}
		
		if (options.isHelp()) {
			printUsage();
			return;
		}
		
		IServerConfiguration serverConfiguration = readServerConfiguration();
		
		if (options.getLogLevel() != null) {
			configureLog(options.getLogLevel(), serverConfiguration);
		} else {			
			configureLog(serverConfiguration.getLogLevel(), serverConfiguration);
		}
		System.setProperty("java.net.preferIPv4Stack", "true");
		
		IServer server = new ServerProxy().start(serverConfiguration);
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// Waiting a while. For that all services has started.
		}
		
		if (options.isConsole()) {
			Thread consoleThread = new Thread(new ConsoleSystem(server.getServerContext()),
					"Granite Server Console Thread");
			consoleThread.start();
		}
	}

	private void configureLog(String logLevel, IServerConfiguration serverConfiguration) {
		System.setProperty("granite.logs.dir", serverConfiguration.getLogsDir());
		
		LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
		
		if (logLevel != null) {			
			if ("debug".equals(logLevel)) {
				configureLog(lc, "logback_debug.xml");
			} else if ("trace".equals(logLevel)) {
				configureLog(lc, "logback_trace.xml");
			} else if ("info".equals(logLevel)) {
				configureLog(lc, "logback.xml");
			} else {
				throw new IllegalArgumentException("Unknown log level option. Only 'info', 'debug' or 'trace' is supported.");
			}
		} else {
			configureLog(lc, "logback.xml");
		}
		
		lc.addTurboFilter(new LogFilter(serverConfiguration.getApplicationLogNamespaces(),
				serverConfiguration.isThirdpartyLogEnabled()));
	}

	private void configureLog(LoggerContext lc, String logFile) {
		configureLC(lc, getClass().getClassLoader().getResource(logFile));
	}

	private void configureLC(LoggerContext lc, URL url) {
		try {
			JoranConfigurator configurator = new JoranConfigurator();
			lc.reset();
			configurator.setContext(lc);
			configurator.doConfigure(url);
		} catch (JoranException e) {
			// ignore, StatusPrinter will handle this
		}
		
	    StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
	}
	
	private IServerConfiguration readServerConfiguration() {
		URL serverJarUrl = Main.class.getProtectionDomain().getCodeSource().getLocation();
		try {
			String serverHome = Paths.get(serverJarUrl.toURI()).getParent().toString();
			return new ServerConfiguration(serverHome);
		} catch (URISyntaxException e) {
			throw new RuntimeException("????", e);
		}
		
	}
	
	private Options parseOptions(String[] args) {
		Options options = new Options();
		
		if (args.length == 1 && args[0].equals("-help")) {
			options.setHelp(true);
			
			return options;
		}
		
		int i = 0;
		while (i < args.length) {
			if ("-console".equals(args[i])) {
				options.setConsole(true);
				i++;
			} else if ("-logLevel".equals(args[i])) {
				if (i == (args.length - 1)) {
					throw new IllegalArgumentException("-logLevel should follow a <LOG_LEVEL> option value.");
				}
				i++;
				
				if (args[i].startsWith("-")) {
					throw new IllegalArgumentException("-logLevel should follow a <LOG_LEVEL> option value.");
				}
				
				if ("info".equals(args[i]) || "debug".equals(args[i]) || "trace".equals(args[i])) {
					options.setLogLevel(args[i]);
				} else {
					throw new IllegalArgumentException("Unknown log level. Only 'info', 'debug', or 'trace' supported. Default is 'info'.");
				}
				i++;
			} else if ("-help".equals(args[i])) {
				throw new IllegalArgumentException("-help should be used alonely.");
			} else {
				throw new IllegalArgumentException(String.format("Unknown option: %s", args[i]));
			}
		}
		
		return options;
	}
	
	private void printUsage() {
		System.out.println("Usage:");
		System.out.println("java -jar granite-server-${VERSION}.jar [OPTIONS]");
		System.out.println("OPTIONS:");
		System.out.println("-help                            Display help information.");
		System.out.println("-console                         Start the server with console.");
		System.out.println("-logLevel <LOG_LEVEL>            Set log level for the server. Three options allowed: 'info', 'debug', 'trace'. Default is 'info'.");
	}
}
