package com.thefirstlineofcode.granite.cluster.node.commons.deploying;

public class Cluster {
	private String domainName;
	private String[] domainAliasNames;
	private String[] nodeTypes;
	
	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	public String[] getDomainAliasNames() {
		return domainAliasNames;
	}

	public void setDomainAliasNames(String[] domainAliasNames) {
		this.domainAliasNames = domainAliasNames;
	}

	public String[] getNodeTypes() {
		return nodeTypes;
	}

	public void setNodeTypes(String[] nodes) {
		this.nodeTypes = nodes;
	}
	
	@Override
	public String toString() {
		return domainName.toString() + "|" + domainAliasNames;
	}
	
}
