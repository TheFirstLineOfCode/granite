package com.thefirstlineofcode.granite.cluster.node.appnode;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlan;
import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlanException;
import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlanReader;
import com.thefirstlineofcode.granite.cluster.node.commons.deploying.NodeType;
import com.thefirstlineofcode.granite.cluster.node.commons.utils.IoUtils;
import com.thefirstlineofcode.granite.cluster.node.commons.utils.ZipUtils;

public class Starter {
	private static final String NAME_PREFIX_GRANITE_SERVER = "granite-server";
	private static final String NAME_POSTFIX_JAR = ".jar";
	private static final String FILE_NAME_DEPLOY_PLAN = "deploy-plan.ini";
	private static final String FILE_NAME_DEPLOY_PLAN_CHECKSUM = "deploy-plan-checksum.txt";
	private static final Logger logger = LoggerFactory.getLogger(Starter.class);
	
	public void start(Options options) {
		MgtnodeIpAndDeployPlanChecksum mgtnodeIpAndDeployPlanChecksum = null;
		if (!options.isNoDeploy()) {
			mgtnodeIpAndDeployPlanChecksum = getMgtnodeIpAndDeployChecksum(options);
			
			if (options.isRedeploy())
				removeLocalDeployPlanCheckSum(options.getConfigurationDir());
			
			String localDeployPlanChecksum = getLocalDeployPlanChecksum(options.getConfigurationDir());
			if (mgtnodeIpAndDeployPlanChecksum != null &&
					mgtnodeIpAndDeployPlanChecksum.deployPlanChecksum != null &&
					!mgtnodeIpAndDeployPlanChecksum.deployPlanChecksum.equals(localDeployPlanChecksum)) {
				if (localDeployPlanChecksum == null) {
					logger.info("Local deploy plan doesn't existed. Trying to deploy appnode...");
				} else {
					logger.info("Deploy plan has changed. Trying to redeploy appnode...");
				}
				
				try {
					downloadDeployPlanFile(mgtnodeIpAndDeployPlanChecksum.mgtnodeIp, options.getMgtnodeHttpPort(),
							options.getConfigurationDir());
					saveDeployPlanChecksum(mgtnodeIpAndDeployPlanChecksum.deployPlanChecksum, options.getConfigurationDir());
				} catch (IOException e) {
					throw new RuntimeException("Can't download deploy plan file.", e);
				}
					
				logger.info("Deploy plan file has downloaded.");
			}
		}
		
		DeployPlan plan = readDeployPlan(options);
		String nodeType = getNodeType(options, plan);
		if (nodeType == null) {
			throw new RuntimeException("Can't determine which node type the appnode is. Please check your deploy plan.");
		}
		
		String runtimeName = getRuntimeName(plan.getChecksum(nodeType));
		
		if ((!isLocalRuntimeZipExisted(options.getRuntimesDir(), runtimeName) || options.isRedeploy()) && mgtnodeIpAndDeployPlanChecksum != null) {
			downloadRuntimeZip(mgtnodeIpAndDeployPlanChecksum.mgtnodeIp, options.getMgtnodeHttpPort(), options.getRuntimesDir(), runtimeName);
			unzipLocalRuntime(options.getRuntimesDir(), runtimeName);
		}
		
		startRuntime(options, nodeType, mgtnodeIpAndDeployPlanChecksum == null ? null : mgtnodeIpAndDeployPlanChecksum.mgtnodeIp, runtimeName);
	}
	
	private void removeLocalDeployPlanCheckSum(String configurationDir) {
		Path localDeployPlanChecksumFilePath = Paths.get(configurationDir, FILE_NAME_DEPLOY_PLAN_CHECKSUM);
		if (Files.exists(localDeployPlanChecksumFilePath)) {
			try {
				Files.delete(localDeployPlanChecksumFilePath);
			} catch (IOException e) {
				throw new RuntimeException("Can't delete local deploy plan checksum file.", e);
			}
		}
	}

	private MgtnodeIpAndDeployPlanChecksum getMgtnodeIpAndDeployChecksum(Options options) {
		String deployPlanChecksum = null;
		String validMgtnodeIp = null;
		if (options.getMgtnodeIp() != null) {
			logger.info("Trying to get deploy plan checksum use address[{}] specified by mgtnode-ip option.",
					options.getMgtnodeIp());
			deployPlanChecksum = tryToGetDeployPlanChecksum(options.getMgtnodeIp(),
					options.getMgtnodeHttpPort());
			
			if (deployPlanChecksum == null) {
				logger.info("Can't get deploy plan checksum use address[{}] specified by mgtnode-ip option.",
						options.getMgtnodeIp());
			} else {
				validMgtnodeIp = options.getMgtnodeIp();
			}
		}
		
		if (deployPlanChecksum == null) {
			logger.info("User didn't configure a static IP for mgtnode. Trying to join cluster to discover mgtnode...");
			joinCluster(options);
			
			ClusterGroup mgtnodes = Ignition.ignite().cluster().forAttribute("ROLE", "mgtnode");
			ClusterNode mgtNode = mgtnodes.node();
			
			if (mgtNode != null) {
				logger.info("Management node found. Trying to get deploy plan checksum file.");
				
				for (String address : mgtNode.addresses()) {
					deployPlanChecksum = tryToGetDeployPlanChecksum(address, options.getMgtnodeHttpPort());
					
					if (deployPlanChecksum != null) {
						validMgtnodeIp = address;
						break;
					}
				}
			}
			
			// We only use ignition cluster to get mgtnode IP address.
			// The task has completed.
			// Stop it.
			Ignition.stop(true);
		}
		
		if (deployPlanChecksum == null)
			logger.info("Can't connect to management node. Ignore to download deploy plan checksum file.");
		else
			logger.info("Deploy plan checksum file has downloaded.");
		
		return deployPlanChecksum == null ? null : new MgtnodeIpAndDeployPlanChecksum(validMgtnodeIp, deployPlanChecksum);
	}

	private class MgtnodeIpAndDeployPlanChecksum {
		public String mgtnodeIp;
		public String deployPlanChecksum;
		
		public MgtnodeIpAndDeployPlanChecksum(String mgtnodeIp, String deployPlanChecksum) {
			this.mgtnodeIp = mgtnodeIp;
			this.deployPlanChecksum = deployPlanChecksum;
		}
	}

	private DeployPlan readDeployPlan(Options options) {
		DeployPlan plan = null;
		try {
			plan = new DeployPlanReader().read(
					new File(options.getConfigurationDir(), FILE_NAME_DEPLOY_PLAN).toPath());
		} catch (DeployPlanException e) {
			throw new RuntimeException("Can't read deploy plan. You need a management node to deploy application node.", e);
		}
		
		return plan;
	}

	private String getNodeType(Options options, DeployPlan plan) {
		String nodeType = filterNodeType(options, plan);
		if (nodeType == null) {
			throw new RuntimeException("Can't determine which type of node the application node should be. Please check your deploy plan.");
		}
		
		return nodeType;
	}
	
	private void startRuntime(Options options, String nodeType, String mgtnodeIp, String runtimeName) {
		List<String> cmdList = new ArrayList<>();
		cmdList.add("java");
		if (options.isRtDebug()) {
			cmdList.add("-Xdebug");
			cmdList.add(String.format("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%s", options.getRtDebugPort()));
		}
		
		if (options.getRtJvmOptions() != null) {
			StringTokenizer st = new StringTokenizer(options.getRtJvmOptions(), " ");
			int tokens = st.countTokens();
			for (int i = 0; i < tokens; i++) {
				cmdList.add(st.nextToken());
			}
		}
		
		cmdList.add("-Dgranite.deploy.plan.file=" + new File(options.getConfigurationDir(), FILE_NAME_DEPLOY_PLAN).getPath());
		cmdList.add("-Dgranite.node.type=" + nodeType);
		
		if (mgtnodeIp != null) {
			cmdList.add("-Dgranite.mgtnode.ip=" + mgtnodeIp);
		}
		
		if (options.getRtLogLevel() != null) {
			cmdList.add("-Dgranite.log.level=" + options.getRtLogLevel());
		}
		
		if (options.isRtLogEnableThirdparties()) {
			cmdList.add("-Dgranite.log.enable.thirdparties=true");
		}
		
		cmdList.add("-jar");
		File runtimeDir = getRuntimeDir(options.getRuntimesDir(), runtimeName);
		if (!runtimeDir.exists()) {
			throw new RuntimeException(String.format("Runtime directory %s doesn't exist.", runtimeDir.getPath()));
		}
		String serverJarName = getServerJarName(runtimeDir);
		cmdList.add(serverJarName);
		
		if (options.isRtConsole()) {
			cmdList.add("-console");
		}
		
		String[] cmdArray = new String[cmdList.size()];
		cmdArray = cmdList.toArray(cmdArray);
		try {
			logger.info("Starting runtime[{}] process...", runtimeName);
			ProcessBuilder pb = new ProcessBuilder(cmdArray).
					redirectInput(Redirect.INHERIT).
					redirectError(Redirect.INHERIT).
					redirectOutput(Redirect.INHERIT).
					directory(runtimeDir);
			Map<String, String> env = pb.environment();
			for (String key : System.getenv().keySet()) {
				env.put(key, System.getenv(key));
			}
			
			Process process = pb.start();
			process.waitFor();
		} catch (IOException e) {
			throw new RuntimeException("Can't run runtime process.", e);
		} catch (InterruptedException e) {
			throw new RuntimeException("Runtime process execution error.", e);
		}
	}

	private String getServerJarName(File runtimeDir) {
		for (File file : runtimeDir.listFiles()) {
			if (file.isFile() &&
					file.getName().startsWith(NAME_PREFIX_GRANITE_SERVER) &&
						file.getName().endsWith(NAME_POSTFIX_JAR)) {
				return file.getName();
			}
		}
		
		return null;
	}
	
	private void unzipLocalRuntime(String runtimesDir, String runtimeName) {
		File localRuntimeDir = new File(runtimesDir, runtimeName);
		if (localRuntimeDir.exists() && !IoUtils.deleteFileRecursively(localRuntimeDir)) {
			throw new RuntimeException(String.format("Can't delete local runtime directory %s.",
					localRuntimeDir.getPath()));
		}
		
		try {
			ZipUtils.unzip(new File(runtimesDir, runtimeName + ".zip"), new File(runtimesDir));
		} catch (IOException e) {
			throw new RuntimeException("Can't unzip runtime.", e);
		}
	}

	private File getRuntimeDir(String runtimesDir, String runtimeName) {
		return new File(runtimesDir, runtimeName);
	}

	private void downloadRuntimeZip(String address, int port, String runtimesDir, String runtimeName) {
		logger.info("Downloading runtime[{}]...", runtimeName);
		
		String runtimeZipName = runtimeName + ".zip";
		File localRuntimeZip = getRuntimeDir(runtimesDir, runtimeZipName);
		
		if (localRuntimeZip.exists()) {
			try {
				Files.delete(localRuntimeZip.toPath());
			} catch (IOException e) {
				throw new RuntimeException(String.format("Can't delete local runtime zip file %s.",
						localRuntimeZip.getPath()));
			}
		}
		
		InputStream in = null;
		BufferedOutputStream out = null;
		try {
			URL url = new URL("HTTP", address, port, "/runtimes/" + runtimeZipName);
			in = new BufferedInputStream(url.openStream());
			
			if (!localRuntimeZip.getParentFile().exists()) {
				Files.createDirectories(localRuntimeZip.getParentFile().toPath());
			}
			Files.createFile(localRuntimeZip.toPath());
			
			out = new BufferedOutputStream(new FileOutputStream(localRuntimeZip));
			
			byte[] buf = new byte[2048];
			int size = -1;
			int totalSize = 0;
			int lastProgress = 0;
			int lastProgressStringLen = 0;
			long lastProgressTime = 0;
			long startTime = System.currentTimeMillis();
			logger.info("Downloading runtime zip...");
			System.out.print("Downloaded: ");
			while ((size = in.read(buf, 0, buf.length)) != -1) {
				out.write(buf, 0, size);
				
				totalSize += size;
				int progress = totalSize / 1024;
				
				long time = System.currentTimeMillis();
				
				if ((time - startTime) > (lastProgressTime + 200) && progress != lastProgress) {
					String progressString = progress + "K.";
					if (lastProgressStringLen != 0) {
						for (int i = 0; i < lastProgressStringLen; i++) {
							System.out.print("\b");
						}
					}
					
					System.out.print(progressString);
					lastProgressTime = time - startTime;
					lastProgress = progress;
					lastProgressStringLen = progressString.length();
				}
			}
			
			int consoleDownloadingProgressStringLen = "Downloaded: ".length() + lastProgressStringLen;
			for (int i = 0; i < consoleDownloadingProgressStringLen; i++) {
				System.out.print("\b");
			}
			logger.info("Downloaded: " + totalSize / 1024 + "K. Done.");
		} catch (Exception e) {
			if (localRuntimeZip.exists()) {
				try {
					Files.delete(localRuntimeZip.toPath());
				} catch (IOException e1) {
					throw new RuntimeException(String.format("Can't delete damaged local runtime zip file. You must delete the file[%s] manually.", localRuntimeZip.getPath()));
				}
			}
			throw new RuntimeException("Can't download runtime zip.", e);
		} finally {
			IoUtils.close(in);
			IoUtils.close(out);
		}
	}

	private boolean isLocalRuntimeZipExisted(String runtimesDir, String runtimeName) {
		return new File(runtimesDir, runtimeName + ".zip").exists();
	}

	private String filterNodeType(Options options, DeployPlan deployPlan) {
		if (options.getNodeType() != null) {
			for (String nodeType : deployPlan.getCluster().getNodeTypes()) {
				if (nodeType.equals(options.getNodeType()))
					return nodeType;
			}
		}
		
		if (deployPlan.getNodeTypes().size() == 1) {
			// There is only one node type in deploy plan.
			// So we select it as appnode's node type.
			return deployPlan.getNodeTypes().keySet().iterator().next();
		}
		
		String filteredNodeType = null;
		for (String nodeType : deployPlan.getNodeTypes().keySet()) {
			if (filterNode(deployPlan.getNodeTypes().get(nodeType), deployPlan)) {
				if (filteredNodeType != null) {
					throw new RuntimeException(String.format(
						"Appnode can be %s or %s node type, but not both. Adjust your deploy plan to guarantee that one appnode corresponds to only one node type.",
							filteredNodeType, nodeType));
				}
				
				filteredNodeType = nodeType;
			}
		}
		
		return filteredNodeType;
	}

	private boolean filterNode(NodeType node, DeployPlan deployPlan) {
		// TODO Provide some mechanisms to filter node type in subsequent version. e.g., IP filter. 
		return false;
	}

	private String getRuntimeName(String checksum) {
		return String.format("rt-%s", checksum);
	}

	private void saveDeployPlanChecksum(String deployPlanChecksum, String configDir) {
		try {
			IoUtils.writeToFile(deployPlanChecksum, Paths.get(configDir, FILE_NAME_DEPLOY_PLAN_CHECKSUM));
		} catch (IOException e) {
			throw new RuntimeException("Can't save deploy plan checksum.", e);
		}
	}

	private void downloadDeployPlanFile(String address, int port, String configDir) throws IOException {
		logger.info("Downloading deploy plan file...");
		
		Path deployPlanFilePath = Paths.get(configDir, FILE_NAME_DEPLOY_PLAN);
		if (Files.exists(deployPlanFilePath)) {
			Path deployPlanBakFilePath = Paths.get(configDir, "deploy-plan.ini.bak");
			if (Files.exists(deployPlanBakFilePath)) {
				Files.delete(deployPlanBakFilePath);
			}
			Files.move(deployPlanFilePath, deployPlanBakFilePath);
		}
		
		InputStream in = null;
		try {
			URL url = new URL("HTTP", address, port, "/deploy/" + FILE_NAME_DEPLOY_PLAN);
			in = url.openStream();
			IoUtils.writeToFile(in, deployPlanFilePath);
		} finally {
			IoUtils.close(in);
		}
	}

	private String getLocalDeployPlanChecksum(String configurationDir) {
		Path localDeployPlanChecksumFilePath = Paths.get(configurationDir, FILE_NAME_DEPLOY_PLAN_CHECKSUM);
		if (Files.exists(localDeployPlanChecksumFilePath)) {
			try {
				return IoUtils.readFile(localDeployPlanChecksumFilePath);
			} catch (IOException e) {
				throw new RuntimeException("Can't read local deploy plan checksum file.", e);
			}
		}
		
		return null;
	}
	
	private String tryToGetDeployPlanChecksum(String address, int port) {
		if (!isValidIpv4Address(address) && !"localhost".equals(address)) {
			logger.debug("Invalid mgtnode address: {}.", address);
			return null;
		}
		
		InputStream in = null;
		try {
			URL url = new URL("HTTP", address, port, "/deploy/" + FILE_NAME_DEPLOY_PLAN_CHECKSUM);
			in = url.openStream();
			byte[] buf = new byte[512];
			int size = in.read(buf, 0, buf.length);
			
			if (size != -1) {
				return new String(buf, 0, size);
			}
		} catch (Exception e) {
			logger.debug("Invalid mgtnode address: {}.", address);
		} finally {
			IoUtils.close(in);
		}
		
		return null;
	}

	private void joinCluster(Options options) {
		logger.info("Application node is trying to join the cluster...");
		
		configureIgniteLogger(options);
		
		System.setProperty("java.net.preferIPv4Stack", "true");
		
		try {
			IgniteConfiguration configuration = new IgniteConfiguration();
			configuration.setUserAttributes(Collections.singletonMap("ROLE", "appnode"));
			
			configuration.setClientMode(true);
			TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
			discoverySpi.setForceServerMode(true);
			configuration.setDiscoverySpi(discoverySpi);
			
			Ignition.start(configuration);
			
			logger.info("Application node has joined the cluster.");
		} catch (Exception e) {
			Ignition.stop(true);
			throw new RuntimeException("Can't join the cluster.", e);
		}
		
	}
	
	private boolean isValidIpv4Address(String address) {
		String[] tokens = address.split("\\.");
		if (tokens.length != 4) {
			return false;
		}
		
		for (String token : tokens) {
			int i = -1;
			try {
				i = Integer.parseInt(token);
			} catch (NumberFormatException e) {
				return false;
			}
			
			if (i < 0 || i > 255)
				return false;
		}
		
		return true;
	}
	
	private void configureIgniteLogger(Options options) {
		System.setProperty("java.util.logging.config.file", options.getConfigurationDir() + "/java_util_logging.ini");
	}
}
