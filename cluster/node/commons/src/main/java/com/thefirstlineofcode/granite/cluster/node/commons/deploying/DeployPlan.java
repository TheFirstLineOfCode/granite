package com.thefirstlineofcode.granite.cluster.node.commons.deploying;

import java.util.HashMap;
import java.util.Map;

import com.thefirstlineofcode.granite.cluster.node.commons.utils.StringUtils;

public class DeployPlan {
	private Cluster cluster;
	private Map<String, NodeType> nodeTypes;
	private Global global;
	private Db db;
	
	public Cluster getCluster() {
		return cluster;
	}
	
	public void setCluster(Cluster cluster) {
		this.cluster = cluster;
	}
	
	public Map<String, NodeType> getNodeTypes() {
		if (nodeTypes == null) {
			nodeTypes = new HashMap<>();
		}
		
		return nodeTypes;
	}
	
	public void setNodeTypes(Map<String, NodeType> nodeTypes) {
		this.nodeTypes = nodeTypes;
	}
	
	public Global getGlobal() {
		return global;
	}

	public void setGlobal(Global global) {
		this.global = global;
	}

	public Db getDb() {
		return db;
	}
	
	public void setDb(Db db) {
		this.db = db;
	}
	
	public String getChecksum() {
		String[] nodeTypeStrings = new String[nodeTypes.size()];
		
		int i = 0;
		for (NodeType nodeType : nodeTypes.values()) {
			nodeTypeStrings[i++] = nodeType.toString();
		}
		
		String[] sortedNodeTypeStrings = StringUtils.sort(nodeTypeStrings);
		
		StringBuilder nodeTypesStringBuilder = new StringBuilder();
		for (String nodeTypeString : sortedNodeTypeStrings) {
			nodeTypesStringBuilder.append(nodeTypeString).append('|');
		}
		nodeTypesStringBuilder.deleteCharAt(nodeTypesStringBuilder.length() - 1);
		
		return StringUtils.getChecksum(cluster.toString() + "-" + global.toString() + "-" + nodeTypesStringBuilder.toString());
	}
	
	public String getAppnodeRuntimeString(String nodeTypeName) {
		NodeType nodeType = getNodeTypes().get(nodeTypeName);
		if (nodeType == null) {
			throw new IllegalArgumentException(String.format("Illegal node type: %s.", nodeTypeName));
		}
		
		String dbString = (db == null) ? null : db.toString();
		
		if (dbString != null && !dbString.isEmpty())
			return cluster.toString() + global.toString() + nodeType.toString() + dbString;
		
		return cluster.toString() + global.toString() + nodeType.toString();
	}
	
	public String getChecksum(String nodeTypeName) {
		return StringUtils.getChecksum(getAppnodeRuntimeString(nodeTypeName));
	}
	
}
