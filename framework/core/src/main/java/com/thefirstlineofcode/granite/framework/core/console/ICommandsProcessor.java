package com.thefirstlineofcode.granite.framework.core.console;

import org.pf4j.ExtensionPoint;

public interface ICommandsProcessor extends ExtensionPoint {
	public static final String DEFAULT_COMMAND_GROUP = "";
	
	String getGroup();
	String[] getCommands();
	String getIntroduction();
	void printHelp(IConsoleSystem consoleSystem);
	boolean process(IConsoleSystem console, String command, String... args) throws Exception;
}
