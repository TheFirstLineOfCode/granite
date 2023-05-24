package com.thefirstlineofcode.granite.framework.core.console;

import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import org.pf4j.Extension;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;

import com.thefirstlineofcode.granite.framework.core.repository.IComponentInfo;
import com.thefirstlineofcode.granite.framework.core.repository.IDependencyInfo;

@Extension
public class DefaultCommandsProcessor extends AbstractCommandsProcessor {
	private static final String COMMANDS_GROUP_INTRODUCTION = "Monitoring and managing granite server.";
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	@Override
	public String getGroup() {
		return ICommandsProcessor.DEFAULT_COMMAND_GROUP;
	}
	
	@Override
	public String[] getCommands() {
		return new String[] {"help", "services", "service", "components", "plugins", "exit", "close"};
	}
	
	void processExit(IConsoleSystem consoleSystem) {
		try {
			consoleSystem.getServerContext().getServer().stop();
		} catch (Exception e) {
			throw new RuntimeException("Can't stop server correctly.", e);
		}
		
		consoleSystem.close();
		
		System.exit(0);
	}
	
	void processClose(IConsoleSystem consoleSystem) {
		consoleSystem.close();		
	}
	
	void processServices(IConsoleSystem consoleSystem) {
		IComponentInfo[] serviceInfos = consoleSystem.getServerContext().getRepository().getServiceInfos();
		
		if (serviceInfos == null || serviceInfos.length == 0)
			consoleSystem.printMessageLine("No any services found.");
		
		consoleSystem.printMessageLine("id\tState\t\tDisabled\tService ID");
		
		for (int i = 0; i < serviceInfos.length; i++) {
			IComponentInfo serviceInfo = serviceInfos[i];
			StringBuilder sb = new StringBuilder();
			sb.append(i);
			sb.append("\t");
			sb.append(serviceInfo.isAvailable() ? "Available  " : "Unavailable");
			sb.append("\t");
			sb.append(isDisabledService(consoleSystem, serviceInfo) ? "Yes" : "No");
			sb.append("\t\t");
			sb.append(serviceInfo.getId());
			consoleSystem.printMessageLine(sb.toString());
		}
	}

	private boolean isDisabledService(IConsoleSystem consoleSystem, IComponentInfo serviceInfo) {
		String[] disabledServices = consoleSystem.getServerContext().getServerConfiguration().getDisabledServices();
		for (String disabledService : disabledServices) {
			if (serviceInfo.getId().equals(disabledService))
				return true;
		}
		
		return false;
	}

	private void printComponentInfos(IConsoleSystem consoleSystem, String title, IComponentInfo[] serviceInfos) {
		consoleSystem.printMessageLine(title);
		
		for (int i = 0; i < serviceInfos.length; i++) {
			IComponentInfo serviceInfo = serviceInfos[i];
			StringBuilder sb = new StringBuilder();
			sb.append(i);
			sb.append("\t");
			sb.append(serviceInfo.isAvailable() ? "Available  " : "Unavailable");
			sb.append("\t");
			sb.append(serviceInfo.getId());
			consoleSystem.printMessageLine(sb.toString());
		}
	}
	
	void processService(IConsoleSystem consoleSystem, String serviceId) {
		IComponentInfo serviceInfo = consoleSystem.getServerContext().getRepository().getServiceInfo(serviceId);
		if (serviceInfo == null) {
			consoleSystem.printMessageLine(String.format("Can't find service by service ID: %s.", serviceId));
			return;
		}
		
		new ServicePrinter(consoleSystem, serviceInfo).print();
	}
	
	void processComponents(IConsoleSystem consoleSystem) {
		IComponentInfo[] componentInfos = consoleSystem.getServerContext().getRepository().getComponentInfos();
		
		if (componentInfos == null || componentInfos.length == 0)
			consoleSystem.printMessageLine("No any components found.");
		
		printComponentInfos(consoleSystem, "id\tState\t\tComponent ID", componentInfos);
	}
	
	void processPlugins(IConsoleSystem consoleSystem) {
		PluginManager pluginManager = consoleSystem.getServerContext().getApplicationComponentService().getPluginManager();
		List<PluginWrapper> plugins = pluginManager.getPlugins();
		
		if (plugins == null || plugins.size() == 0)
			consoleSystem.printMessageLine("No any plugins found.");
		
		plugins.sort(new Comparator<PluginWrapper>() {
			@Override
			public int compare(PluginWrapper pw1, PluginWrapper pw2) {
				return pw1.getPluginId().compareTo(pw2.getPluginId());
			}
		});
		
		consoleSystem.printMessageLine("id\tState\t\tPlugin ID");
		
		for (int i = 0; i < plugins.size(); i++) {
			PluginWrapper plugin = plugins.get(i);
			StringBuilder sb = new StringBuilder();
			sb.append(i);
			sb.append("\t");
			sb.append(plugin.getPluginState());
			sb.append("\t\t");
			sb.append(plugin.getPluginId());
			consoleSystem.printMessageLine(sb.toString());
		}
	}
	
	@Override
	public void printHelp(IConsoleSystem consoleSystem) {
		consoleSystem.printTitleLine(String.format("%s Available commands:", getIntroduction()));
		consoleSystem.printContentLine("help [COMMAND_GROUP_NAME] - Display help information.");
		consoleSystem.printContentLine("services - List all services.");
		consoleSystem.printContentLine("service <SERVICE_ID> - Display details for the specified service.");
		consoleSystem.printContentLine("components - List all components.");
		consoleSystem.printContentLine("plugins - List all plugins.");
		consoleSystem.printContentLine("close - Close the console.");
		consoleSystem.printContentLine("exit - Stop the server and exit system.");
		
		if (consoleSystem.getCommandsProcessors().length == 1)
			return;
		
		consoleSystem.printBlankLine();
		consoleSystem.printTitleLine("Call help [COMMAND_GROUP_NAME] below for more available commands contributed by plugins:");
		for (ICommandsProcessor commandsProcessor : consoleSystem.getCommandsProcessors()) {
			if (commandsProcessor.getGroup().equals(ICommandsProcessor.DEFAULT_COMMAND_GROUP))
				continue;
			
			consoleSystem.printContentLine(String.format("help %s - %s", commandsProcessor.getGroup(),
					commandsProcessor.getIntroduction()));
		}
	}
	
	private class ServicePrinter {
		private IConsoleSystem consoleSystem;
		private IComponentInfo service;
		
		public ServicePrinter(IConsoleSystem consoleSystem, IComponentInfo service) {
			this.consoleSystem = consoleSystem;
			this.service = service;
		}
		
		public void print() {
			consoleSystem.printMessage(getServiceDetails());
		}

		private String getServiceDetails() {
			StringBuilder sb = new StringBuilder();
			
			if (!service.isAvailable()) {
				sb.append("*");
			}
			sb.append(service.getId());
			sb.append(LINE_SEPARATOR);
			
			if (service.getDependencies().length > 0) {
				Stack<Integer> hierarchyContext = new Stack<>();
				hierarchyContext.push(getBindingsCountOfAllDependencies(service));
				
				writeDependencies(sb, service.getId(), service.getDependencies(), hierarchyContext);
				
				hierarchyContext.pop();
			}
			
			return sb.toString();
		}

		private int getBindingsCountOfAllDependencies(IComponentInfo component) {
			int count = 0;
			for (IDependencyInfo dependency : component.getDependencies()) {
				String[] bindings = consoleSystem.getServerContext().getRepository().getComponentBinding(component.getId() + "$" + dependency.getBareId());
				count += (bindings == null ? 1 : bindings.length);
			}
			
			return count;
		}

		private void writeDependencies(StringBuilder sb, String parentId, IDependencyInfo[] dependencies,
					Stack<Integer> hierarchyContext) {
			
			for (IDependencyInfo dependency : dependencies) {
				String[] bindings = consoleSystem.getServerContext().getRepository().getComponentBinding(parentId + "$" + dependency.getBareId());
				if (bindings != null && bindings.length > 0) {
					for (String binding : bindings) {
						hierarchyContext.push(hierarchyContext.pop() - 1);
						writeBinding(sb, dependency, binding, hierarchyContext);
					}
				} else {
					hierarchyContext.push(hierarchyContext.pop() - 1);
					writeHierarchyLine(sb, hierarchyContext);
					sb.append('?');
					sb.append(dependency.getBareId());
					sb.append(LINE_SEPARATOR);
				}
			}
		}

		private void writeBinding(StringBuilder sb, IDependencyInfo dependency, String binding, Stack<Integer> hierarchyContext) {
			writeHierarchyLine(sb, hierarchyContext);
			
			IComponentInfo binded = findBindedComponent(dependency, binding);
			if (binded == null) {
				sb.append('!');
			} else if (!binded.isAvailable()) {
				sb.append('*');
			}
			
			sb.append(dependency.getBareId());
			sb.append("->");
			sb.append(binding);
			sb.append(LINE_SEPARATOR);
			
			if (binded != null) {
				hierarchyContext.push(getBindingsCountOfAllDependencies(binded));
				writeDependencies(sb, binded.getId(), binded.getDependencies(), hierarchyContext);
				hierarchyContext.pop();
			}
		}

		private IComponentInfo findBindedComponent(IDependencyInfo dependency, String binding) {			
			for (IComponentInfo binded : dependency.getBindedComponents()) {
				if (binded.getId().equals(binding)) {
					return binded;
				}
			}
			
			return null;
		}

		private void writeHierarchyLine(StringBuilder sb, Stack<Integer> hierarchyContext) {
			for (int i = 0; i < hierarchyContext.size() - 1; i++) {
				if (hierarchyContext.get(i) > 0) {
					sb.append('|').append("  ");
				} else {
					sb.append("   ");
				}
			}
			
			if (hierarchyContext.peek() == 0) {
				sb.append('\\');
			} else {
				sb.append('+');
			}
			
			sb.append("- ");
		}
	}

	@Override
	public String getIntroduction() {
		return COMMANDS_GROUP_INTRODUCTION;
	}
}
