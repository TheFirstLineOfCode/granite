package com.thefirstlineofcode.granite.cluster.node.mgtnode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;

import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlan;
import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlanException;
import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlanReader;
import com.thefirstlineofcode.granite.cluster.node.commons.utils.IoUtils;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.AppnodeRuntimesPacker;
import com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.IAppnodeRuntimesPacker;

public class Starter implements Serializable {
	private static final String FILE_NAME_DEPLOY_PLAN_CHECKSUM = "deploy-plan-checksum.txt";

	private static final long serialVersionUID = -6753095799926138420L;

	private static final String FILE_NAME_DEPLOY_PLAN = "deploy-plan.ini";
	
	private static final Logger logger = LoggerFactory.getLogger(Starter.class);

	// Jetty HTTP Server
	private Server server;
	
	private ConsoleThread consoleThread;
	
	public boolean start(Options options) throws Exception {
		checkRepository(options.getRepositoryDir());
		DeployPlan deployPlan = readAndCheckDeployConfiguration(options.getConfigurationDir(),
				options.getDeployDir(), options.isRepack());
		if (deployPlan == null)
			return false;
		
		checkAndPackAppnodeRuntimes(options, deployPlan);
		startJettyServer(options);
		
		synchronized (this) {
			wait();
		}
		
		if (server.isStarted()) {
			logger.info("HTTP Server has started.");
		} else {
			logger.error("Can't start HTTP Server.");
			return false;
		}
		
		boolean clusterJoined = joinCluster(options);
		
		try {
			Thread.sleep(500);
		} catch (Exception e) {
			// ignore
		}
		
		if (!clusterJoined) {
			logger.error("Can't join the cluster.");
			return false;
		}
		
		logger.info("Starting console...");
		startConsoleThread();
		
		return true;
	}
	
	private DeployPlan readAndCheckDeployConfiguration(String configurationDir, String deployDir, boolean repack) {
		DeployPlan deployPlan = readDeployPlan(configurationDir);
		if (deployPlan == null)
			return null;
		
		saveDeployPlanToDeployPath(configurationDir, deployDir, deployPlan, repack);
		
		return deployPlan;
	}

	private void saveDeployPlanToDeployPath(String configDir, String deployDir, DeployPlan deployPlan, boolean repack) {
		File deployDirFile = new File(deployDir);
		if (!deployDirFile.exists()) {
			logger.info("Deploy directory doesn't exist. Creating it...");
			
			try {
				Files.createDirectories(deployDirFile.toPath());
			} catch (IOException e) {
				throw new RuntimeException("Can't create deploy directory.", e);
			}
			
			logger.info("Deploy directory has created.");
		}
		
		logger.info("Checking checksum file to determine if deploy plan changed.");
		boolean planChanged = false;
		Path localDeployPlanChecksumFilePath = Paths.get(deployDir, FILE_NAME_DEPLOY_PLAN_CHECKSUM);
		
		if (repack && Files.exists(localDeployPlanChecksumFilePath)) {
			try {
				Files.delete(localDeployPlanChecksumFilePath);
			} catch (IOException e) {
				throw new RuntimeException("Can't delete local deploy plan checksum file.", e);
			}
		}
		
		if (!Files.exists(localDeployPlanChecksumFilePath)) {
			planChanged = true;
		} else {
			try {
				String localDeployPlanChecksum = IoUtils.readFile(localDeployPlanChecksumFilePath);
				if (!deployPlan.getChecksum().equals(localDeployPlanChecksum)) {
					planChanged = true;
				}
			} catch (IOException e) {
				throw new RuntimeException("Can't read local deploy plan checksum text file.", e);
			}
		}
		
		if (!planChanged) {
			logger.info("The deploy plan hasn't changed. Ignore to redeploy it.");
			return;
		}
		
		try {
			logger.info("Copying deploy plan file to deploy directory.");
			Files.copy(new File(configDir, FILE_NAME_DEPLOY_PLAN).toPath(), 
					new File(deployDirFile, FILE_NAME_DEPLOY_PLAN).toPath(),
						StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException("Can't copy deploy plan file to deploy directory.", e);
		}
		logger.info("Deploy plan file has copied to deploy directory.");
		
		try {
			logger.info("Saving the deploy plan checksum to local.");
			IoUtils.writeToFile(deployPlan.getChecksum(), localDeployPlanChecksumFilePath);
		} catch (IOException e) {
			throw new RuntimeException("Can't write the deploy plan checksum to local.", e);
		}
		logger.info("Deploy plan checksum file has written.");
	}

	private DeployPlan readDeployPlan(String configurationDir) {
		File deployPlanFile = new File(configurationDir, FILE_NAME_DEPLOY_PLAN);
		
		if (!deployPlanFile.exists()) {
			logger.error("Can't read {}. It doesn't exist.", deployPlanFile.getPath());
			return null;
		}
		
		logger.info("Reading deploy plan...");
		DeployPlan deployPlan = null;
		try {
			deployPlan =  new DeployPlanReader().read(deployPlanFile.toPath());
		} catch (DeployPlanException e) {
			throw new RuntimeException("Can't parse deploy plan.", e);
		}
		logger.info("Deploy plan has read.");
		
		if (deployPlan.getCluster() == null) {
			logger.error("Invalid deploy plan file. Must include a custer section");
			return null;
		}
		
		if (deployPlan.getCluster().getDomainName() == null) {
			logger.error("Invalid deploy plan file. Domain name must be defined.");
			return null;
		}
		
		if (deployPlan.getNodeTypes().isEmpty()) {
			logger.error("Invalid deploy plan file. No node type defined.");
			return null;
		}
		
		return deployPlan;
	}

	private void checkRepository(String repositoryDirPath) {
		logger.info("Checking repository...");
		File repositoryDir = new File(repositoryDirPath);
		if (!repositoryDir.exists()) {
			throw new RuntimeException(String.format("Repository directory %s doesn't exist.", repositoryDir));
		}
		
		if (!repositoryDir.isDirectory()) {
			throw new RuntimeException(String.format("Repository directory %s isn't a valid directory.", repositoryDir));
		}
		logger.info("Repository is fine.");
	}

	private void checkAndPackAppnodeRuntimes(Options options, DeployPlan deployPlan) {
		File appnodeRuntimesDir = new File(options.getAppnodeRuntimesDir());
		if (!appnodeRuntimesDir.exists()) {
			logger.info("Appnode runtimes directory doesn't exist. Creating it...");
			
			try {
				Files.createDirectories(appnodeRuntimesDir.toPath());
			} catch (IOException e) {
				throw new RuntimeException(String.format("Can't create runtimes directory[%s]", appnodeRuntimesDir.getPath()), e);
			}
			
			logger.info("Appnode runtimes directory {} has created.", appnodeRuntimesDir.getPath());
		}
		
		packAppnodeRuntimes(options, deployPlan);
	}

	private void packAppnodeRuntimes(Options options, DeployPlan deployPlan) {
		IAppnodeRuntimesPacker deployer = new AppnodeRuntimesPacker(options);
		for (String nodeType : deployPlan.getNodeTypes().keySet()) {
			String checksum = deployPlan.getChecksum(nodeType);
			String appnodeRuntimeName = getAppnodeRuntimeName(checksum);
			
			logger.info("Ready to pack appnode runtime[{}]. Runtime checksum is {}.", nodeType, checksum);
			deployer.pack(nodeType, appnodeRuntimeName, deployPlan);
			logger.info("Appnode runtime[{}] has packed.", nodeType);
		}
	}

	private String getAppnodeRuntimeName(String checksum) {
		return String.format("rt-%s", checksum);
	}

	private void startConsoleThread() {
		consoleThread = new ConsoleThread();
		new Thread(consoleThread, "Management Node Console Thread").start();
	}
	
	private class ConsoleThread implements Runnable {
		private volatile boolean stop = false;
		
		@Override
		public void run() {
			printConsoleHelp();
			
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				try {
					String command = readCommand(in);
					
					if (stop)
						break;
					
					if ("help".equals(command)) {
						printConsoleHelp();
					} else if ("exit".equals(command)) {
						exitSystem();
					} else {
						System.out.println(String.format("Unknown command: '%s'", command));
						printConsoleHelp();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		private String readCommand(BufferedReader in) throws IOException {
			while (!in.ready()) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (stop) {
					return null;
				}
			}
			
			return in.readLine();
		}
	}

	private void exitSystem() {
		if (server != null) {
			try {
				server.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			server.destroy();
		}
		
		if (consoleThread != null) {
			consoleThread.stop = true;
		}
		
		Ignition.stop(true);
	}

	private void printConsoleHelp() {
		System.out.println("Commands:");
		System.out.println("help        Display help information.");
		System.out.println("exit        Exit system.");
		System.out.print("$");
	}

	private boolean joinCluster(Options options) {
		logger.info("Management node is trying to join the cluster...");
		
		configureJavaUtilLogging(options);
		
		System.setProperty("java.net.preferIPv4Stack", "true");
		
		try {
			IgniteConfiguration configuration = new IgniteConfiguration();
			configuration.setUserAttributes(Collections.singletonMap("ROLE", "mgtnode"));
			
			configuration.setClientMode(true);
			TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
			discoverySpi.setForceServerMode(true);
			configuration.setDiscoverySpi(discoverySpi);
			
			Ignition.start(configuration);
			
			logger.info("Management node has joined the cluster.");
			
			return true;
		} catch (Exception e) {
			logger.error("Can't join the cluster. System will exit.", e);
			exitSystem();
			
			return false;
		}
		
	}

	private void configureJavaUtilLogging(Options options) {
		System.setProperty("java.util.logging.config.file", options.getConfigurationDir() + "/java_util_logging.ini");
	}

	private void startJettyServer(Options options) throws Exception {
		Thread httpServerThread = new Thread(new HttpServerThread(options), "Management Node HTTP Server Thread");
		httpServerThread.start();
	}
	
	private class HttpServerThread implements Runnable {
		private Options options;
		
		public HttpServerThread(Options options) {
			this.options = options;
		}
		
		@Override
		public void run() {
			try {
				server = new Server(options.getHttpPort());
				
				HandlerList contextHandlers = new HandlerList();
				contextHandlers.setHandlers(new Handler[] {createDeployContextHandler(), createRuntimesContextHandler()});
				
				server.setHandler(contextHandlers);
				logger.info("Starting HTTP Server...");
				server.start();
				server.dumpStdErr();
				logger.info("HTTP Server has started on port {}.", options.getHttpPort());
			} catch (Exception e) {
				try {
					server.stop();
				} catch (Exception e1) {
					logger.error("Can't stop http server.", e);
				}
				server.destroy();
				logger.error("Some exceptions occurred in http server thread. System will exit.", e);
			} finally {
				synchronized (Starter.this) {
					Starter.this.notify();
				}
			}
		}

		private ContextHandler createRuntimesContextHandler() {
			ResourceHandler runtimesResourceHandler = new ResourceHandler();
			runtimesResourceHandler.setResourceBase(options.getAppnodeRuntimesDir());
			runtimesResourceHandler.setDirectoriesListed(false);
			HandlerList runtimesHandlers = new HandlerList();
			runtimesHandlers.setHandlers(new Handler[] {runtimesResourceHandler, new DefaultHandler()});
			
			ContextHandler runtimesContextHandler = new ContextHandler();
			runtimesContextHandler.setContextPath("/runtimes");
			runtimesContextHandler.setHandler(runtimesHandlers);
			
			return runtimesContextHandler;
		}

		private ContextHandler createDeployContextHandler() {
			ResourceHandler deployResourceHandler = new ResourceHandler();
			deployResourceHandler.setResourceBase(options.getDeployDir());
			deployResourceHandler.setDirectoriesListed(false);
			HandlerList deployHandlers = new HandlerList();
			deployHandlers.setHandlers(new Handler[] {deployResourceHandler, new DefaultHandler()});
			
			ContextHandler deployContextHandler = new ContextHandler();
			deployContextHandler.setContextPath("/deploy");
			deployContextHandler.setHandler(deployHandlers);
			
			return deployContextHandler;
		}
	}
}
