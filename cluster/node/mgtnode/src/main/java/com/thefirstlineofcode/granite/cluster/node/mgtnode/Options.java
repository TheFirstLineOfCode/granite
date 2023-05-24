package com.thefirstlineofcode.granite.cluster.node.mgtnode;

import com.thefirstlineofcode.granite.cluster.node.commons.options.OptionsBase;

public class Options extends OptionsBase {
	private int httpPort;
	private String repositoryDir;
	private String deployDir;
	private String appnodeRuntimesDir;
	private boolean repack;
	
	public Options() {
		httpPort = 8090;
		repack = false;
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
	
}
