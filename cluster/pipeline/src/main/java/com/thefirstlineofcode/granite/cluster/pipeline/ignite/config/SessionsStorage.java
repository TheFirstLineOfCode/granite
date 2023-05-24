package com.thefirstlineofcode.granite.cluster.pipeline.ignite.config;

public class SessionsStorage extends Storage {
	public static final String NAME_SESSIONS_STORAGE = "sessions-storage";
	
	private int sessionDurationTime;
	
	public SessionsStorage() {
		initSize = 32 * 1024 * 1024;
		maxSize = 128 * 1024 * 1024;
		backups = 1;
		persistenceEnabled = true;
		sessionDurationTime = 5 * 60;
	}

	public int getSessionDurationTime() {
		return sessionDurationTime;
	}

	public void setSessionDurationTime(int sessionDurationTime) {
		this.sessionDurationTime = sessionDurationTime;
	}
	
}
