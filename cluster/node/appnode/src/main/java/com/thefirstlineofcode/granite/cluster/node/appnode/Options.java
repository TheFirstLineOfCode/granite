package com.thefirstlineofcode.granite.cluster.node.appnode;

import com.thefirstlineofcode.granite.cluster.node.commons.options.OptionsBase;

public class Options extends OptionsBase {
	private String mgtnodeIp;
	private int mgtnodeHttpPort;
	private boolean redeploy;
	private boolean noDeploy;
	private String runtimesDir;
	private boolean rtDebug;
	private int rtDebugPort;
	private String rtJvmOptions;
	private String rtLogLevel;
	private boolean rtLogEnableThirdparties;
	private boolean rtConsole;
	private String nodeType;
	
	public Options() {
		redeploy = false;
		noDeploy = false;
		mgtnodeHttpPort = 8090;
		rtDebug = false;
		rtDebugPort = 8020;
		rtLogEnableThirdparties = false;
		rtConsole = false;
	}
	
	public String getMgtnodeIp() {
		return mgtnodeIp;
	}
	
	public void setMgtnodeIp(String mgtnodeIp) {
		this.mgtnodeIp = mgtnodeIp;
	}
	
	public int getMgtnodeHttpPort() {
		return mgtnodeHttpPort;
	}

	public void setMgtnodeHttpPort(int mgtnodeHttpPort) {
		this.mgtnodeHttpPort = mgtnodeHttpPort;
	}
	
	public boolean isRedeploy() {
		return redeploy;
	}

	public void setRedeploy(boolean redeploy) {
		this.redeploy = redeploy;
	}

	public boolean isNoDeploy() {
		return noDeploy;
	}

	public void setNoDeploy(boolean noDeploy) {
		this.noDeploy = noDeploy;
	}

	public void setRuntimesDir(String runtimesDir) {
		this.runtimesDir = runtimesDir;
	}
	
	public String getRuntimesDir() {
		return runtimesDir;
	}

	public boolean isRtDebug() {
		return rtDebug;
	}

	public void setRtDebug(boolean rtDebug) {
		this.rtDebug = rtDebug;
	}

	public int getRtDebugPort() {
		return rtDebugPort;
	}

	public void setRtDebugPort(int rtDebugPort) {
		this.rtDebugPort = rtDebugPort;
	}

	public String getRtJvmOptions() {
		return rtJvmOptions;
	}

	public void setRtJvmOptions(String rtJvmOptions) {
		this.rtJvmOptions = rtJvmOptions;
	}

	public boolean isRtConsole() {
		return rtConsole;
	}

	public void setRtConsole(boolean rtConsole) {
		this.rtConsole = rtConsole;
	}

	public String getRtLogLevel() {
		return rtLogLevel;
	}

	public void setRtLogLevel(String rtLogLevel) {
		this.rtLogLevel = rtLogLevel;
	}

	public boolean isRtLogEnableThirdparties() {
		return rtLogEnableThirdparties;
	}

	public void setRtLogEnableThirdparties(boolean rtLogEnableThirdparties) {
		this.rtLogEnableThirdparties = rtLogEnableThirdparties;
	}

	public String getNodeType() {
		return nodeType;
	}

	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}
	
}
