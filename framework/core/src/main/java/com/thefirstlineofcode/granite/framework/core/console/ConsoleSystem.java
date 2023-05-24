package com.thefirstlineofcode.granite.framework.core.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.pf4j.PluginManager;

import com.thefirstlineofcode.granite.framework.core.IServerContext;
import com.thefirstlineofcode.granite.framework.core.adf.ApplicationComponentService;

public class ConsoleSystem implements Runnable, IConsoleSystem {
	private static final char CHAR_BLANK_SPACE = ' ';
	private static final String COMMAND_HELP = "help";

	private volatile boolean stop = false;
	
	private IServerContext serverContext;
	private Map<String, ICommandsProcessor> commandsProcessors;
	
	public ConsoleSystem(IServerContext serverContext) {
		this.serverContext = serverContext;
		commandsProcessors = new HashMap<>();
	}
	
	@Override
	public void run() {
		loadContributedCommandsProcessors();
		
		printBlankLine();
		printTitleLine("\tGranite Server Console");		
		printBlankLine();
		
		printDefaultHelp();
		printBlankLine();
		printPrompt();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			try {
				if (stop)
					break;
				
				String input = in.readLine().trim();
				
				if (input == null) {
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					continue;
				}
				
				printBlankLine();
				
				String group = null;
				String command = null;
				String[] args = null;
				int blankSpaceIndex = input.indexOf(CHAR_BLANK_SPACE);
				if (blankSpaceIndex != -1) {
					command = input.substring(0, blankSpaceIndex);
					if (commandsProcessors.keySet().contains(command)) {
						group = command;
						String commandAndArgs = input.substring(blankSpaceIndex + 1);
						blankSpaceIndex = commandAndArgs.indexOf(CHAR_BLANK_SPACE);
						if (blankSpaceIndex == -1) {
							command = commandAndArgs; 
							args = new String[0];
						} else {
							command = commandAndArgs.substring(0, blankSpaceIndex);
							String sArgs = commandAndArgs.substring(blankSpaceIndex + 1);
							args = getArgs(sArgs);
						}
					} else {
						if (isDefaultCommands(command)) {
							group = ICommandsProcessor.DEFAULT_COMMAND_GROUP;
							args = getArgs(input.substring(blankSpaceIndex + 1));
						} else {
							printMessageLine(String.format("Unknown command group: '%s'", group));
							printDefaultHelp();							
						}
					}
				} else {
					group = ICommandsProcessor.DEFAULT_COMMAND_GROUP;
					command = input;
					args = new String[0];
				}
				
				if (group != null && command != null && args != null) {					
					try {
						if (!processCommand(group, command, args)) {
							printBlankLine();
							printDefaultHelp();
						}
					} catch (Exception e) {					
						e.printStackTrace();
					}
				}
				
				printBlankLine();
				printPrompt();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	private String[] getArgs(String sArgs) {
		String[] args;
		StringTokenizer st = new StringTokenizer(sArgs, String.valueOf(CHAR_BLANK_SPACE));							
		args = new String[st.countTokens()];
		for (int i = 0; i < args.length; i++) {
			args[i] = st.nextToken();
		}
		return args;
	}
	
	private boolean isDefaultCommands(String command) {
		for (String aCommand : getDefaultCommandsProcessor().getCommands()) {
			if (command.equals(aCommand)) {
				return true;
			}
		}
		
		return false;
	}

	private void printDefaultHelp() {
		try {
			doPrintDefaultHelp();
		} catch (Exception e) {
			printBlankLine();
			printMessageLine("Something was wrong. Close system now.");
			e.printStackTrace(getOutputStream());
			try {
				closeSystem();
			} catch (Exception exception) {
				throw new RuntimeException("Can't close system correctly.", exception);
			}
		}
	}

	private void closeSystem() throws Exception {
		getDefaultCommandsProcessor().process(this, "close");
	}

	private ICommandsProcessor getDefaultCommandsProcessor() {
		return commandsProcessors.get(ICommandsProcessor.DEFAULT_COMMAND_GROUP);
	}

	private void loadContributedCommandsProcessors() {
		ICommandsProcessor defaultCommandsProcessor = new DefaultCommandsProcessor();
		commandsProcessors.put(defaultCommandsProcessor.getGroup(), defaultCommandsProcessor);
		
		ApplicationComponentService appComponentService = (ApplicationComponentService)serverContext.getApplicationComponentService();
		PluginManager pluginManager = appComponentService.getPluginManager();
		List<? extends ICommandsProcessor> contributedCommandsProcessors = pluginManager.getExtensions(ICommandsProcessor.class);
		for (ICommandsProcessor contributedCommandsProcessor : contributedCommandsProcessors) {
			String group = contributedCommandsProcessor.getGroup();
			
			if (group == null) {
				throw new IllegalArgumentException("Null command group.");
			}
			
			if (commandsProcessors.containsKey(group)) {
				throw new IllegalArgumentException(String.format("Reduplicate command group: '%s'.", group));
			}
			
			if (contributedCommandsProcessor instanceof IConsoleSystemAware)
				((IConsoleSystemAware)contributedCommandsProcessor).setConsoleSystem(this);
			
			commandsProcessors.put(group, contributedCommandsProcessor);
		}
	}

	private boolean processCommand(String group, String command, String... args) {
		if (group == null) {
			group = ICommandsProcessor.DEFAULT_COMMAND_GROUP;
		}
		
		ICommandsProcessor commandsProcessor = commandsProcessors.get(group);
		if (commandsProcessor == null) {
			printBlankLine();
			printMessageLine(String.format("Unknown command group: '%s'", group));
			
			return false;
		}
		
		if (COMMAND_HELP.equals(command)) {
			printHelp(commandsProcessor, args);
			return true;
		}
		
		for (String aCommand : commandsProcessor.getCommands()) {
			if (aCommand.equals(command)) {
				try {
					return commandsProcessor.process(this, command, args);
				} catch (Exception e) {
					printBlankLine();
					printMessageLine("Can't process the command. Exception was thrown.");
					e.printStackTrace(getOutputStream());
				}
				
				return true;
			}
		}
		
		if (ICommandsProcessor.DEFAULT_COMMAND_GROUP.equals(group)) {
			printBlankLine();
			printMessageLine(String.format("Unknown command: '%s'", command));			
		} else {
			printBlankLine();
			printMessageLine(String.format("Unknown command: '%s %s'", group, command));
		}
		
		return false;
	}

	private void printHelp(ICommandsProcessor commandsProcessor, String[] args) {
		if (commandsProcessor.getGroup().equals(ICommandsProcessor.DEFAULT_COMMAND_GROUP) &&
				(args != null && args.length == 1)) {
			ICommandsProcessor commandsProcessorForHelp = commandsProcessors.get(args[0]);
			if (commandsProcessorForHelp == null) {
				printBlankLine();
				printMessageLine("Unknown command group: " + args[0]);
				printDefaultHelp();
				
				return;
			}
			
			commandsProcessorForHelp.printHelp(this);
			return;
		}
		
		if (args != null && args.length != 0) {
			printBlankLine();
			printMessageLine("Help command is only called with no arguments.");
			
			return;
		}
		
		printHelp(commandsProcessor);
	}

	private void printHelp(ICommandsProcessor commandsProcessor) {
		commandsProcessor.printHelp(this);
	}
	
	public void stop() {
		stop = true;
	}

	private void doPrintDefaultHelp() throws Exception {
		try {
			getDefaultCommandsProcessor().printHelp(this);
		} catch (Exception e) {
			// Why???. Ignore it.
		}
	}
	
	@Override
	public void printPrompt() {
		printMessage("$ ");		
	}
	
	@Override
	public IServerContext getServerContext() {
		return serverContext;
	}
	
	@Override
	public void printBlankLine() {
		if (!stop)
			System.out.println();
	}

	@Override
	public void printMessage(String message) {
		if (!stop)
			System.out.print(message);
	}

	@Override
	public PrintStream getOutputStream() {
		return System.out;
	}
	
	@Override
	public void printTitleLine(String title) {
		printMessageLine(title);
	}
	
	@Override
	public void printContentLine(String content) {
		printMessageLine("  " + content);
	}

	@Override
	public void printMessageLine(String message) {
		if (!stop)
			System.out.println(message);
	}
	
	public void close() {
		stop = true;
	}

	@Override
	public ICommandsProcessor[] getCommandsProcessors() {
		return commandsProcessors.values().toArray(new ICommandsProcessor[commandsProcessors.size()]);
	}
}
