package com.thefirstlineofcode.granite.pack.cluster.mgtnode;

public class Options {
	private boolean help;
	private String version;
	private String appName;
	private String targetDirPath;
	private String graniteProjectDirPath;
	private String projectDirPath;
	private String repositoryDirPath;
	private boolean update;
	private boolean cleanCache;
	private boolean cleanUpdate;
	private String[] modules;
	private boolean offline;
	private boolean commerical;
	
	public Options() {
		help = false;
		update = false;
		cleanCache = false;
		cleanUpdate = false;
		offline = false;
		commerical = false;
	}
	
	public boolean isHelp() {
		return help;
	}
	
	public void setHelp(boolean help) {
		this.help = help;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
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

	public String getProjectDirPath() {
		return projectDirPath;
	}

	public void setProjectDirPath(String projectDirPath) {
		this.projectDirPath = projectDirPath;
	}

	public String getRepositoryDirPath() {
		return repositoryDirPath;
	}

	public void setRepositoryDirPath(String repositoryDir) {
		this.repositoryDirPath = repositoryDir;
	}

	public boolean isUpdate() {
		return update;
	}

	public void setUpdate(boolean update) {
		this.update = update;
	}
	
	public boolean isPack() {
		return !isUpdate() && !isCleanUpdate();
	}
	
	public String[] getModules() {
		return modules;
	}

	public void setModules(String[] bundles) {
		this.modules = bundles;
	}
	
	public boolean isCleanUpdate() {
		return cleanUpdate;
	}

	public void setCleanUpdate(boolean cleanUpdate) {
		this.cleanUpdate = cleanUpdate;
	}
	
	public boolean isCleanCache() {
		return cleanCache;
	}
	
	public void setCleanCache(boolean cleanCache) {
		this.cleanCache = cleanCache;
	}
	
	public boolean isOffline() {
		return offline;
	}

	public void setOffline(boolean offline) {
		this.offline = offline;
	}

	public boolean isCommerical() {
		return commerical;
	}
	
	public void setCommerical(boolean commerical) {
		this.commerical = commerical;
	}
	
}
