package com.thefirstlineofcode.granite.cluster.pipeline.ignite.config;

public class ResourcesStorage extends Storage {
	public static final String NAME_RESOURCES_STORAGE = "resources-storage";
	
	public ResourcesStorage() {
		initSize = 8 * 1024 * 1024;
		maxSize = 32 * 1024 * 1024;
		backups = 1;
		persistenceEnabled = true;
	}

}
