package com.thefirstlineofcode.granite.cluster.nodes.commons.options;

public class StringOptionSetter extends AbstractOptionSetter {
	public void setOption(OptionsBase options, String name, String value) {
		setPropertyToOptions(options, name, value);
	}
}