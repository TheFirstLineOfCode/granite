package com.thefirstlineofcode.granite.cluster.node.commons.options;

public class IntegerOptionSetter extends AbstractOptionSetter {
	public void setOption(OptionsBase options, String name, String value) {
		Integer intValue = new IntTypeConverter().convert(name, value);
		setPropertyToOptions(options, name, intValue);
	}
}