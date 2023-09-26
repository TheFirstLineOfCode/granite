package com.thefirstlineofcode.granite.cluster.nodes.mgtnode;

import java.util.Map;

import com.thefirstlineofcode.granite.cluster.nodes.commons.options.AbstractOptionsTool;
import com.thefirstlineofcode.granite.cluster.nodes.commons.options.OptionRule;

public class OptionsTool extends AbstractOptionsTool<Options> {

	public OptionsTool() {
		super("mgtnode.ini");
	}

	@Override
	protected Map<String, OptionRule> buildOptionRules(Map<String, OptionRule> optionRules) {
		optionRules.put("http-port",
				new OptionRule().
					setRange(OptionRule.Range.BOTH).
					setDataType(OptionRule.DataType.INTEGER));
		
		optionRules.put("repository-dir",
				new OptionRule().
					setRange(OptionRule.Range.BOTH).
					setDataType(OptionRule.DataType.STRING));
		
		optionRules.put("repack",
				new OptionRule().
					setRange(OptionRule.Range.BOTH).
					setDataType(OptionRule.DataType.BOOLEAN));
		
		optionRules.put("debug",
				new OptionRule().
					setRange(OptionRule.Range.BOTH).
					setDataType(OptionRule.DataType.BOOLEAN));
		
		optionRules.put("debug-port",
				new OptionRule().
					setRange(OptionRule.Range.BOTH).
					setDataType(OptionRule.DataType.INTEGER));
		
		optionRules.put("run-mode",
				new OptionRule().
				setRange(OptionRule.Range.COMMAND_LINE).
				setDataType(OptionRule.DataType.STRING));
		
		return optionRules;
	}

	@Override
	protected Options createOptions() {
		return new Options();
	}

	@Override
	protected void printUsage() {
		System.out.println("Usage: java -jar granite-cluster-node-mgtnode-${VERSION}.jar [OPTIONS]");
		System.out.println("OPTIONS:");
		System.out.println("--help                                 Display help information.");
		System.out.println("--http-port=HTTP_PORT                  Specify the http port that mgt node to use.");
		System.out.println("--configuration-dir=CONFIGURATION_DIR  Specify the path of configuration directory.");
		System.out.println("--repository-dir=REPOSITORY_DIR        Specify the path of repository directory.");
		System.out.println("--repack                               Repack the runtime packages.");
		System.out.println("--debug                                Run JDWP.");
		System.out.println("--debug-port                           Repack the runtime packages.");
	}

}
