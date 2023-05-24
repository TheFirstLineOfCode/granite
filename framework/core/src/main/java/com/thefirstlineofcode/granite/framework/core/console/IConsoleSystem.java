package com.thefirstlineofcode.granite.framework.core.console;

import java.io.PrintStream;

import com.thefirstlineofcode.granite.framework.core.IServerContext;

public interface IConsoleSystem {
	IServerContext getServerContext();
	PrintStream getOutputStream();
	ICommandsProcessor[] getCommandsProcessors();
	
	void printBlankLine();
	void printMessage(String message);
	void printMessageLine(String message);
	void printTitleLine(String title);
	void printContentLine(String content);
	void printPrompt();
	
	void close();
}
