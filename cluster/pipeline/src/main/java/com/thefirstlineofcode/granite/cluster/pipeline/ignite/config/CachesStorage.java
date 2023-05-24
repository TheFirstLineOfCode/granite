package com.thefirstlineofcode.granite.cluster.pipeline.ignite.config;

public class CachesStorage extends Storage {
	public enum EvictionPolicy {
		RANDOM_LRU,
		RANDOM_2_LRU
	}
	
	public static final String NAME_CACHES_STORAGE = "caches-storage";
	
	private EvictionPolicy evicationPolicy;

	public CachesStorage() {
		initSize = 64 * 1024 * 1024;
		maxSize = 256 * 1024 * 1024;
		backups = 1;
		persistenceEnabled = false;
		evicationPolicy = EvictionPolicy.RANDOM_2_LRU;
	}
	
	public EvictionPolicy getEvicationPolicy() {
		return evicationPolicy;
	}

	public void setEvicationPolicy(EvictionPolicy evicationPolicy) {
		this.evicationPolicy = evicationPolicy;
	}
	
}
