package com.thefirstlineofcode.granite.cluster.node.commons.deploying;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.thefirstlineofcode.granite.cluster.node.commons.utils.StringUtils;

public class NodeType {
	private String[] abilities;
	private String[] protocols;
	private Map<String, Properties> configurations;
	
	public NodeType() {
		configurations = new HashMap<>();
	}
	
	public String[] getAbilities() {
		if (abilities == null)
			return new String[0];
		
		return abilities;
	}
	
	public void setAbilities(String[] abilities) {
		this.abilities = abilities;
	}
	
	public String[] getProtocols() {
		if (protocols == null)
			return new String[0];
		
		return protocols;
	}
	
	public void setProtocols(String[] protocols) {
		this.protocols = protocols;
	}
	
	@Override
	public String toString() {
		return getNodeTypeString();
	}
	
	public void setConfiguration(String featureName, Properties properties) {
		configurations.put(featureName, properties);
	}
	
	public Properties getConfiguration(String featureName) {
		Properties configuration = null;
		configuration = configurations.get(featureName);
		
		if (configuration == null) {
			configuration = new Properties();
		}
		
		return configuration;
	}

	private String getNodeTypeString() {
		String abilitiesString = getAbilitiesString();
		String protocolsString = getProtocolsString();
		
		return abilitiesString + "|" + protocolsString;
	}

	private String getProtocolsString() {
		String[] sortedProtocols = StringUtils.sort(getProtocols());
		StringBuilder sb = new StringBuilder();
		sb.append("protocols[");
		for (String protocol : sortedProtocols) {
			Properties configuration = configurations.get("protocol-" + protocol);
			String configurationString = getConfigurationString(configuration);
			
			sb.append(protocol);
			if (configurationString != null) {
				sb.append('[').
					append(configurationString).
				append(']');
			}
			sb.append(',');
		}
		
		if (sb.length() > 0) {
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append(']');
		
		return sb.toString();
	}

	private String getConfigurationString(Properties configuration) {
		if (configuration == null || configuration.isEmpty())
			return null;
		
		Set<Object> keySet = configuration.keySet();
		String[] keys = keySet.toArray(new String[keySet.size()]);
		keys = StringUtils.sort(keys);
		
		StringBuilder sb = new StringBuilder();
		for (String key : keys) {
			sb.append(key).
				append('=').
				append(configuration.get(key)).
				append(',').append(' ');
		}
		
		if (sb.length() > 0) {
			sb.delete(sb.length() - 2, sb.length());
		}
		
		return sb.toString();
	}

	private String getAbilitiesString() {
		String[] sortedAbilities = StringUtils.sort(getAbilities());
		StringBuilder sb = new StringBuilder();
		sb.append("abilities[");
		for (String ability : sortedAbilities) {
			Properties configuration = configurations.get("ability-" + ability);
			String configurationString = getConfigurationString(configuration);
			
			sb.append(ability);
			if (configurationString != null) {
				sb.append('[').
					append(configurationString).
				append(']');
			}
			sb.append(',');
		}
		
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		sb.append(']');
		
		return sb.toString();
	}
	
	public boolean hasAbility(String ability) {
		for (String anAbility : getAbilities()) {
			if (anAbility.equals(ability))
				return true;
		}
		
		return false;
	}
	
}
