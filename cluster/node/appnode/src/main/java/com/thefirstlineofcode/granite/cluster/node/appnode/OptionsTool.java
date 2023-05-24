package com.thefirstlineofcode.granite.cluster.node.appnode;


import java.util.Map;

import com.thefirstlineofcode.granite.cluster.node.commons.options.AbstractOptionsTool;
import com.thefirstlineofcode.granite.cluster.node.commons.options.OptionRule;
import com.thefirstlineofcode.granite.cluster.node.commons.options.OptionRule.DataType;
import com.thefirstlineofcode.granite.cluster.node.commons.options.OptionRule.Range;

public class OptionsTool extends AbstractOptionsTool<Options> {
	public OptionsTool() {
		super("appnode.ini");
	}
	
	@Override
	protected Map<String, OptionRule> buildOptionRules(Map<String, OptionRule> optionRules) {
		optionRules.put("mgtnode-ip",
				new OptionRule().
				setRange(Range.BOTH).
				setDataType(DataType.STRING));
		optionRules.put("mgtnode-http-port",
				new OptionRule().
					setRange(Range.BOTH).
					setDataType(DataType.INTEGER));
		optionRules.put("runtimes-dir",
				new OptionRule().
				setRange(Range.BOTH).
				setDataType(DataType.STRING));
		optionRules.put("redeploy",
				new OptionRule().
				setRange(Range.BOTH).
				setDataType(DataType.BOOLEAN));
		optionRules.put("no-deploy",
				new OptionRule().
				setRange(Range.BOTH).
				setDataType(DataType.BOOLEAN));
		optionRules.put("node-type",
				new OptionRule().
				setRange(Range.BOTH).
				setDataType(DataType.STRING));
		optionRules.put("rt-debug",
				new OptionRule().
				setRange(Range.BOTH).
				setDataType(DataType.BOOLEAN));
		optionRules.put("rt-debug-port",
				new OptionRule().
				setRange(Range.BOTH).
				setDataType(DataType.INTEGER));
		optionRules.put("rt-jvm-options",
				new OptionRule().
				setRange(Range.BOTH).
				setDataType(DataType.STRING));
		optionRules.put("rt-log-level",
				new OptionRule().
				setRange(Range.BOTH).
				setDataType(DataType.STRING));
		optionRules.put("rt-log-enable-thirdparties",
				new OptionRule().
				setRange(Range.BOTH).
				setDataType(DataType.BOOLEAN));
		optionRules.put("rt-console",
				new OptionRule().
				setRange(Range.BOTH).
				setDataType(DataType.BOOLEAN));
		
		return optionRules;
	}
	
	@Override
	protected Options createOptions() {
		return new Options();
	}
	
	@Override
	protected void printUsage() {
		System.out.println("Usage: java granite-cluster-node-appnode-${VERSION}.jar [OPTIONS]");
		System.out.println("OPTIONS:");
		System.out.println("--help                                 Display help information.");
		System.out.println("--runtimes-dir=RUNTIMES_DIR            Specify the path of runtimes directory.");
		System.out.println("--configuration-dir=CONFIGURATION_DIR  Specify the path of configuration directory.");
		System.out.println("--mgtnode-ip=IP                        Specify the mgtnode ip address that appnode to connect.");
		System.out.println("--mgtnode-http-port=HTTP_PORT          Specify the mgtnode http port that appnode to connect.");
		System.out.println("--redeploy                             Force to redeploy runtime from mgtnode.");
		System.out.println("--no-deploy                            Don't deploy runtime from mgtnode.");
		System.out.println("--node-type=NODE_TYPE                  Specify the node type which the node should deploy.");
		System.out.println("--rt-debug                             Run runtime process in debug mode.");
		System.out.println("--rt-debug-port=RT_DEBUG_PORT          Specify JDWP port the runtime process listens in debug mode.");
		System.out.println("--rt-jvm-options=RT_JVM_OPTIONS        Specify additional JVM options the runtime process uses.");
		System.out.println("--rt-log-level=RT_LOG_LEVEL            Specify log level the runtime process uses.");
		System.out.println("--rt-log-enable-thirdparties           Enable thirdparty libraries's log in runtime process.");
		System.out.println("--rt-console                           Open the runtime process console.");
	}

}