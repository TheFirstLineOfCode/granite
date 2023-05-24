package com.thefirstlineofcode.granite.cluster.pipeline.ignite.config;

public class Storage {
	protected long initSize;
	protected long maxSize;
	protected int backups;
	protected boolean persistenceEnabled;
	
	public long getInitSize() {
		return initSize;
	}
	
	public void setInitSize(long initSize) {
		this.initSize = initSize;
	}
	
	public long getMaxSize() {
		return maxSize;
	}
	
	public void setMaxSize(long maxSize) {
		this.maxSize = maxSize;
	}
	
	public int getBackups() {
		return backups;
	}

	public void setBackups(int backups) {
		this.backups = backups;
	}

	public boolean isPersistenceEnabled() {
		return persistenceEnabled;
	}

	public void setPersistenceEnabled(boolean persistenceEnabled) {
		this.persistenceEnabled = persistenceEnabled;
	}
	
}
