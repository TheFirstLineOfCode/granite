package com.thefirstlineofcode.granite.cluster.node.commons.options;

public class OptionsBase {
	private boolean help;
	private String homeDir;
	private String configurationDir;
	
	public void setHelp(boolean help) {
		this.help = help;
	}
	
	public boolean isHelp() {
		return help;
	}
	
	public String getHomeDir() {
		return homeDir;
	}

	public void setHomeDir(String homeDir) {
		this.homeDir = homeDir;
	}

	public String getConfigurationDir() {
		return configurationDir;
	}

	public void setConfigurationDir(String configurationDir) {
		this.configurationDir = configurationDir;
	}

}
