package com.thefirstlineofcode.granite.cluster.pipeline.ignite.config;

public class Discovery {
	public enum Strategy {
		MULTICAST,
		STATIC_IP,
		MULTICAST_AND_STATIC_IP
	}
	
	public static final String NAME_DISCOVERY = "discovery";
	
	private Strategy strategy;
	private String multicastGroup;
	private boolean useMgtnodeStaticIp;
	private String[] staticAddresses;
	
	public Discovery() {
		strategy = Strategy.MULTICAST;
		multicastGroup = "228.10.10.157";
		useMgtnodeStaticIp = false;
		staticAddresses = null;
	}
	
	public Strategy getStrategy() {
		return strategy;
	}

	public void setStrategy(Strategy strategy) {
		this.strategy = strategy;
	}

	public String getMulticastGroup() {
		return multicastGroup;
	}
	
	public void setMulticastGroup(String multicastGroup) {
		this.multicastGroup = multicastGroup;
	}
	
	public boolean isUseMgtnodeStaticIp() {
		return useMgtnodeStaticIp;
	}
	
	public void setUseMgtnodeStaticIp(boolean useMgtnodeStaticIp) {
		this.useMgtnodeStaticIp = useMgtnodeStaticIp;
	}
	
	public String[] getStaticAddresses() {
		return staticAddresses;
	}
	
	public void setStaticAddresses(String[] staticAddresses) {
		this.staticAddresses = staticAddresses;
	}
	
}
