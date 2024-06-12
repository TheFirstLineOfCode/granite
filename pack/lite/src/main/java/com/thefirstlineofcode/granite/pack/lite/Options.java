package com.thefirstlineofcode.granite.pack.lite;

public class Options {
	private boolean help;
	private boolean update;
	private boolean cleanCache;
	private boolean cleanUpdate;
	private String version;
	private String[] modules;
	private String targetDirPath;
	private String graniteProjectDirPath;
	private String sandProjectDirPath;
	private String projectDirPath;
	private Protocol protocol;
	private boolean commerical;
	private boolean offline;
	private String appName;
	private WebcamMode webcamMode;
	
	public enum WebcamMode {
		NONE,
		P2P,
		KURENTO
	}
	
	public enum Protocol {
		MINI,
		STANDARD,
		IOT,
		SAND_DEMO
	}
	
	public Options() {
		help = false;
		update = false;
		cleanCache = false;
		cleanUpdate = false;
		protocol = Protocol.STANDARD;
		commerical = false;
		offline = false;
		webcamMode = null;
	}
	
	public boolean isHelp() {
		return help;
	}

	public void setHelp(boolean help) {
		this.help = help;
	}

	public boolean isUpdate() {
		return update;
	}
	
	public void setUpdate(boolean update) {
		this.update = update;
	}
	
	public boolean isCleanCache() {
		return cleanCache;
	}
	
	public void setCleanCache(boolean cleanCache) {
		this.cleanCache = cleanCache;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}

	public String[] getModules() {
		return modules;
	}

	public void setModules(String[] bundles) {
		this.modules = bundles;
	}

	public String getTargetDirPath() {
		return targetDirPath;
	}

	public void setTargetDirPath(String targetDirPath) {
		this.targetDirPath = targetDirPath;
	}

	public String getGraniteProjectDirPath() {
		return graniteProjectDirPath;
	}

	public void setGraniteProjectDirPath(String graniteDir) {
		this.graniteProjectDirPath = graniteDir;
	}
	
	public String getSandProjectDirPath() {
		return sandProjectDirPath;
	}

	public void setSandProjectDirPath(String sandDir) {
		this.sandProjectDirPath = sandDir;
	}

	public String getProjectDirPath() {
		return projectDirPath;
	}

	public void setProjectDirPath(String projectDirPath) {
		this.projectDirPath = projectDirPath;
	}
	
	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}
	
	public Protocol getProtocol() {
		return protocol;
	}

	public boolean isCleanUpdate() {
		return cleanUpdate;
	}

	public void setCleanUpdate(boolean cleanUpdate) {
		this.cleanUpdate = cleanUpdate;
	}
	
	public boolean isPack() {
		return !isUpdate() && !isCleanUpdate();
	}
	
	public boolean isCommerical() {
		return commerical;
	}
	
	public void setCommerical(boolean commerical) {
		this.commerical = commerical;
	}

	public boolean isOffline() {
		return offline;
	}

	public void setOffline(boolean offline) {
		this.offline = offline;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public WebcamMode getWebcamMode() {
		return webcamMode;
	}

	public void setWebcamMode(WebcamMode webcamMode) {
		this.webcamMode = webcamMode;
	}
	
}
