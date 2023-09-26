package com.thefirstlineofcode.granite.cluster.nodes.mgtnode;

import com.thefirstlineofcode.granite.cluster.nodes.commons.options.OptionsBase;

public class Options extends OptionsBase {
	public static final String RUN_MODE_LANUCHER = "lanucher";
	public static final String RUN_MODE_PROCESS = "process";
	
	private int httpPort;
	private String repositoryDir;
	private String deployDir;
	private String appnodeRuntimesDir;
	private boolean repack;
	private boolean debug;
	private int debugPort;
	private String runMode;
	
	public Options() {
		httpPort = 8090;
		repack = false;
		debug = false;
		debugPort = 8000;
		runMode = RUN_MODE_LANUCHER;
	}
	
	public void setHttpPort(int httpPort) {
		this.httpPort = httpPort;
	}
	
	public int getHttpPort() {
		return httpPort;
	}

	public String getRepositoryDir() {
		return repositoryDir;
	}

	public void setRepositoryDir(String repositoryDir) {
		this.repositoryDir = repositoryDir;
	}
	
	public void setAppnodeRuntimesDir(String runtimesDir) {
		this.appnodeRuntimesDir = runtimesDir;
	}
	
	public String getAppnodeRuntimesDir() {
		return appnodeRuntimesDir;
	}

	public String getDeployDir() {
		return deployDir;
	}

	public void setDeployDir(String deployDir) {
		this.deployDir = deployDir;
	}

	public boolean isRepack() {
		return repack;
	}

	public void setRepack(boolean repack) {
		this.repack = repack;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public int getDebugPort() {
		return debugPort;
	}

	public void setDebugPort(int debugPort) {
		this.debugPort = debugPort;
	}

	public String getRunMode() {
		return runMode;
	}

	public void setRunMode(String runMode) {
		this.runMode = runMode;
	}
	
	public boolean isLanucherRunMode() {
		return RUN_MODE_LANUCHER.equals(runMode);
	}
	
	public boolean isProcessRunMode() {
		return RUN_MODE_PROCESS.equals(runMode);
	}
}
