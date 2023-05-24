package com.thefirstlineofcode.granite.cluster.node.commons.options;

public class OptionRule {
	
	public enum Range {
		BOTH,
		COMMAND_LINE,
		CONFIG_FILE
	}
	
	public enum DataType {
		STRING {
			public OptionSetter getOptionSetter() {
				return new StringOptionSetter();
			}
		},
		INTEGER {
			public OptionSetter getOptionSetter() {
				return new IntegerOptionSetter();
			}
		},
		BOOLEAN {
			public OptionSetter getOptionSetter() {
				return new BooleanOptionSetter();
			}
		};
		
		public OptionSetter getOptionSetter() {
			return null;
		}
	}
	
	private Range range;
	private DataType dataType;
	
	public OptionRule setDataType(DataType dataType) {
		this.dataType = dataType;
		
		return this;
	}
	
	public OptionRule setRange(Range range) {
		this.range = range;
		return this;
	}
	
	public OptionSetter getOptionSetter() {
		return dataType.getOptionSetter();
	}
	
	public Range getRange() {
		return range;
	}
	
}
