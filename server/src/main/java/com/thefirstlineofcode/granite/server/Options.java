package com.thefirstlineofcode.granite.server;

public class Options {
	private String logLevel;
	private boolean console;
	private boolean help;
	
	public String getLogLevel() {
		return logLevel;
	}
	
	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}
	
	public boolean isConsole() {
		return console;
	}
	
	public void setConsole(boolean console) {
		this.console = console;
	}

	public boolean isHelp() {
		return help;
	}

	public void setHelp(boolean help) {
		this.help = help;
	}
	
}
