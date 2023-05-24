package com.thefirstlineofcode.granite.cluster.node.commons.deploying;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.thefirstlineofcode.granite.cluster.node.commons.utils.SectionalProperties;
import com.thefirstlineofcode.granite.cluster.node.commons.utils.StringUtils;

import java.util.Properties;
import java.util.StringTokenizer;

public class DeployPlanReader implements IDeployPlanReader {
	private static final String SECTION_NAME_GLOBAL = "global";
	private static final String PROPERTY_NAME_DB_NAME = "db-name";
	private static final String PROPERTY_NAME_PASSWORD = "password";
	private static final String PROPERTY_NAME_USERNAME = "user-name";
	private static final String PROPERTY_NAME_ADDRESSES = "addresses";
	private static final String PROPERTY_NAME_PROTOCOLS = "protocols";
	private static final String PROPERTY_NAME_ABILITIES = "abilities";
	private static final String SECTION_NAME_DB = "db";
	private static final String SECTION_NAME_CLUSTER = "cluster";
	private static final String PROPERTY_NAME_NODE_TYPES = "node-types";
	private static final String PROPERTY_NAME_DOMAIN_ALIAS_NAMES = "domain-alias-names";
	private static final String PROPERTY_NAME_DOMAIN_NAME = "domain-name";
	private static final String[] NODE_DEFAULT_ABILITIES = new String[] {"stream", "processing", "event"};
	
	@Override
	public DeployPlan read(Path deployPlanPath) throws DeployPlanException {
		File deployPlanFile = deployPlanPath.toFile();
		if (!deployPlanFile.exists()) {
			throw new DeployPlanException(String.format("Deploy plan file %s doesn't exist.",
					deployPlanFile.getPath()));
		}
		
		SectionalProperties sp = new SectionalProperties();
		try {
			sp.load(new FileInputStream(deployPlanFile));
		} catch (IOException e) {
			throw new DeployPlanException(String.format("Can't read deploy plan file %s.",
					deployPlanFile.getPath()), e);
		}
		
		DeployPlan deployPlan = new DeployPlan();
		
		Properties clusterProp = sp.getSection(SECTION_NAME_CLUSTER);
		if (clusterProp == null) {
			throw new DeployPlanException("Deploy plan file must include a cluster section.");
		}
		Cluster cluster = readClusterSection(clusterProp);
		deployPlan.setCluster(cluster);
		
		Properties globalProp = sp.getSection(SECTION_NAME_GLOBAL);
		Global global;
		if (globalProp == null) {
			global = new Global();
		} else {
			global = readGlobalSection(globalProp);
		}
		deployPlan.setGlobal(global);
		
		Properties dbProp = sp.getSection(SECTION_NAME_DB);
		if (dbProp == null) {
			throw new DeployPlanException("Deploy plan file must include a db section.");
		} 
		Db db = readDbSection(dbProp);
		deployPlan.setDb(db);
		
		List<String> processedSections = new ArrayList<>();
		processedSections.add(SECTION_NAME_CLUSTER);
		processedSections.add(SECTION_NAME_GLOBAL);
		processedSections.add(SECTION_NAME_DB);
		
		for (String nodeTypeName : deployPlan.getCluster().getNodeTypes()) {
			NodeType nodeType = readNodeTypeSection(nodeTypeName, cluster, sp.getSection(nodeTypeName));
			if (nodeType == null) {
				throw new DeployPlanException(String.format("Node configuration section lost. Node type name: %s.", nodeType));
			}
			
			deployPlan.getNodeTypes().put(nodeTypeName, nodeType);
			processedSections.add(nodeTypeName);
		}
		
		for (String sectionName : sp.getSectionNames()) {
			if (processedSections.contains(sectionName))
				continue;
			
			NodeTypeAndFeature ntaf = getNodeTypeAndFeature(deployPlan, sectionName);
			deployPlan.getNodeTypes().get(ntaf.nodeTypeName).setConfiguration(
					ntaf.featureName, sp.getSection(sectionName));
		}
		
		return deployPlan;
	}
	
	private Global readGlobalSection(Properties properties) throws DeployPlanException {
		Global global = new Global();
		
		String sSessionDurationTime = properties.getProperty("session-duration-time");
		if (sSessionDurationTime != null) {
			try {
				global.setSessionDurationTime(Integer.parseInt(sSessionDurationTime));
			} catch (NumberFormatException e) {
				throw new DeployPlanException("Property 'session-duration-time' in global section must be an integer.");
			}
		}
		
		String messageFormat = properties.getProperty("message-format");
		if (messageFormat != null) {
			
			if (Global.MESSAGE_FORMAT_BINARY.equals(messageFormat) || Global.MESSAGE_FORMAT_XML.equals(messageFormat)) {
				global.setMessageFormat(messageFormat);
			}
			
			throw new DeployPlanException("Property 'message-format' must be 'xml' or 'binary'.");
		}
		
		return global;
	}

	private class NodeTypeAndFeature {
		public String nodeTypeName;
		public String featureName;
		
		public NodeTypeAndFeature(String nodeTypeName, String featureName) {
			this.nodeTypeName = nodeTypeName;
			this.featureName = featureName;
		}	
	}

	private NodeTypeAndFeature getNodeTypeAndFeature(DeployPlan configuration, String featureSectionName) throws DeployPlanException {
		String nodeTypeName = stripNodeTypeName(configuration, featureSectionName);
		NodeType nodeType = configuration.getNodeTypes().get(nodeTypeName);
		if (nodeType == null) {
			throw new DeployPlanException(String.format("Illegal feature section. Can't determine node type from feature section name '%s'.", featureSectionName));
		}
		
		String featureName = featureSectionName.substring(nodeTypeName.length() + 1, featureSectionName.length());
		if (featureName.startsWith("ability-")) {
			String pureFeatureName = featureName.substring(8, featureName.length());
			if (!checkAbilityFeature(nodeType, pureFeatureName)) {
				throw new DeployPlanException(String.format("Illegal feature section name '%s'. Ability '%s' can't be found in node type '%s'.",
						featureSectionName, pureFeatureName, nodeType));
			}
			
			return new NodeTypeAndFeature(nodeTypeName, featureName);
		} else if (featureName.startsWith("protocol-")) {
			String pureFeatureName = featureName.substring(9, featureName.length());
			if (!checkProtocolFeature(nodeType, pureFeatureName)) {
				throw new DeployPlanException(String.format("Illegal feature section name '%s'. Protocol '%s' can't be found in node type '%s'.",
						featureSectionName, pureFeatureName, nodeType));
			}
			
			return new NodeTypeAndFeature(nodeTypeName, featureName);
		} else if ("*".equals(featureName)) {
			return new NodeTypeAndFeature(nodeTypeName, featureName);
		} else {
			boolean isAbility = false;
			boolean isProtocol = false;
			
			for (String ability : nodeType.getAbilities()) {
				if (featureName.equals(ability)) {
					isAbility = true;
					break;
				}
			}
			
			for (String protocol : nodeType.getProtocols()) {
				if (featureName.equals(protocol)) {
					isProtocol = true;
					break;
				}
			}
			
			if (!isAbility && !isProtocol) {
				throw new DeployPlanException(String.format("Illegal feature section name '%s'. The feature '%s' can't find in node type '%s'.",
						featureSectionName, featureName, nodeTypeName));
			}
			
			if (isAbility && isProtocol) {
				throw new DeployPlanException(String.format("Ambiguous feature section name '%s'. Can't determine it's feature type.", featureSectionName));
			}
			
			if (isAbility) {
				return new NodeTypeAndFeature(nodeTypeName, "ability-" + featureName);
			} else {
				return new NodeTypeAndFeature(nodeTypeName, "protocol-" + featureName);
			}
		}
	}

	private boolean checkProtocolFeature(NodeType nodeType, String protocolName) throws DeployPlanException {
		for (String protocol : nodeType.getAbilities()) {
			if (protocolName.equals(protocol)) {
				return true;
			}
		}
		
		return false;
	}

	private boolean checkAbilityFeature(NodeType nodeType, String abilityName) throws DeployPlanException {
		for (String ability : nodeType.getAbilities()) {
			if (abilityName.equals(ability)) {
				return true;
			}
		}
		
		return false;
	}

	private String stripNodeTypeName(DeployPlan configuration, String featureName) {
		for (String nodeTypeName : configuration.getCluster().getNodeTypes()) {
			if (featureName.startsWith(nodeTypeName + "-")) {
				return nodeTypeName;
			}
		}
		
		return null;
	}

	private NodeType readNodeTypeSection(String nodeTypeName, Cluster cluster, Properties properties) throws DeployPlanException {
		if (properties == null)
			return null;
		
		NodeType nodeType = new NodeType();
		for (Entry<Object, Object> entry : properties.entrySet()) {
			String key = (String)entry.getKey();
			if (PROPERTY_NAME_ABILITIES.equals(key)) {
				nodeType.setAbilities(StringUtils.stringToArray((String)entry.getValue()));
			} else if (PROPERTY_NAME_PROTOCOLS.equals(key)) {
				nodeType.setProtocols(StringUtils.stringToArray((String)entry.getValue()));
			} else {
				throw new DeployPlanException(String.format("Invalid property in 'node' section. Property name: %s.", key));
			}
		}
		
		if (nodeType.getAbilities() == null || nodeType.getAbilities().length == 0) {
			nodeType.setAbilities(NODE_DEFAULT_ABILITIES);
		}
		
		// can deploy a node doesn't support any protocols?
		/*if (node.getProtocols() == null) {
			throw new DeployConfigurationException("Property 'protocols' lost in node configuration section.");
		}*/
		
		return nodeType;
	}

	private Db readDbSection(Properties properties) throws DeployPlanException {
		Db db = new Db();
		for (Entry<Object, Object> entry : properties.entrySet()) {
			String key = (String)entry.getKey();
			if (PROPERTY_NAME_ADDRESSES.equals(key)) {
				db.setAddresses(getDbAddresses((String)entry.getValue()));
			} else if (PROPERTY_NAME_USERNAME.equals(key)) {
				db.setUserName((String)entry.getValue());
			} else if (PROPERTY_NAME_PASSWORD.equals(key)) {
				db.setPassword(((String)entry.getValue()).getBytes());
			} else if (PROPERTY_NAME_DB_NAME.equals(key)) {
				db.setDbName((String)entry.getValue());
			} else {
				throw new DeployPlanException(String.format("Invalid property in 'db' section. Property name: %s.", key));
			}
		}
		
		if (db.getAddresses() == null || db.getAddresses().isEmpty()) {
			throw new  DeployPlanException("Property 'addresses' in 'db' section must be set.");
		}
		
		if (db.getDbName() == null) {
			throw new  DeployPlanException("Property 'db-name' in 'db' section must be set.");
		}
		
		if (db.getUserName() == null) {
			throw new  DeployPlanException("Property 'user-name' in 'db' section must be set.");
		}
		
		if (db.getPassword() == null) {
			throw new  DeployPlanException("Property 'password' in 'db' section must be set.");
		}
		
		return db;
	}

	private List<DbAddress> getDbAddresses(String sAddresses) throws DeployPlanException {
		StringTokenizer st = new StringTokenizer(sAddresses, ",");
		List<DbAddress> addresses = new ArrayList<>();
		
		while (st.hasMoreTokens()) {
			String sAddress = st.nextToken();
			int colonIndex = sAddress.indexOf(':');
			if (colonIndex != -1) {
				String host = sAddress.substring(0, colonIndex).trim();
				int port;
				try {
					port = Integer.parseInt(sAddress.substring(colonIndex + 1, sAddress.length()));
				} catch (NumberFormatException e) {
					throw new DeployPlanException(String.format("Invalid DB addresses: %s", sAddresses));
				}
				
				addresses.add(new DbAddress(host, port));
			} else {
				addresses.add(new DbAddress(sAddress, 27017));
			}
			
		}
		
		return addresses;
	}

	private Cluster readClusterSection(Properties properties) throws DeployPlanException {
		Cluster cluster = new Cluster();
		for (Object key : properties.keySet()) {
			String value = properties.getProperty((String)key);
			
			if (PROPERTY_NAME_DOMAIN_NAME.equals(key)) {
				cluster.setDomainName(value);
			} else if (PROPERTY_NAME_DOMAIN_ALIAS_NAMES.equals(key)) {
				cluster.setDomainAliasNames(StringUtils.stringToArray(value));
			} else if (PROPERTY_NAME_NODE_TYPES.equals(key)) {
				cluster.setNodeTypes(StringUtils.stringToArray(value));
			} else {
				throw new DeployPlanException(String.format("Invalid property in 'cluster' section. Property name: %s.", key));
			}
		}
		
		return cluster;
	}
	
}
