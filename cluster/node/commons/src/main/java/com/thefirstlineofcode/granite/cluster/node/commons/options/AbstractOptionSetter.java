package com.thefirstlineofcode.granite.cluster.node.commons.options;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

public abstract class AbstractOptionSetter implements OptionSetter {
	protected String optionName2PropertyName(String name) {
		while (true) {
			int dashIndex = name.indexOf('-');
			if (dashIndex == -1)
				return name;
			
			// convert kebab-case to camelCase
			name = name.substring(0, dashIndex) +
					Character.toUpperCase(name.charAt(dashIndex + 1)) +
						((name.length() > dashIndex + 1) ? name.substring(dashIndex + 2, name.length()) : "");
		}
	}

	protected void setPropertyToOptions(OptionsBase options, String optionName, Object optionValue) {
		String propertyName = optionName2PropertyName(optionName);
		try {
			PropertyDescriptor propertyDescriptor = new PropertyDescriptor(propertyName, options.getClass());
			Method writter = propertyDescriptor.getWriteMethod();
			writter.invoke(options, optionValue);
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Unable to set value of property % to object %s",
					options.getClass().getName(), propertyName));
		}
		
	}
}
