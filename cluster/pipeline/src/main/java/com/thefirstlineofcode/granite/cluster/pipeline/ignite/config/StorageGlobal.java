package com.thefirstlineofcode.granite.cluster.pipeline.ignite.config;

public class StorageGlobal {
	public static final String NAME_STORAGE_GLOBAL = "storage-global";
	
	private String workDirectory;
	private int pageSize;
	private String storagePath;
	private String walPath;
	private String walArchivePath;
	
	public String getWorkDirectory() {
		return workDirectory;
	}

	public void setWorkDirectory(String workDirectory) {
		this.workDirectory = workDirectory;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public String getStoragePath() {
		return storagePath;
	}

	public void setStoragePath(String storagePath) {
		this.storagePath = storagePath;
	}

	public String getWalPath() {
		return walPath;
	}

	public void setWalPath(String walPath) {
		this.walPath = walPath;
	}

	public String getWalArchivePath() {
		return walArchivePath;
	}

	public void setWalArchivePath(String walArchivePath) {
		this.walArchivePath = walArchivePath;
	}
	
}
