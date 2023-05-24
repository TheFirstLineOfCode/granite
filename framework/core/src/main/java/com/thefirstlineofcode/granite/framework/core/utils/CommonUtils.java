package com.thefirstlineofcode.granite.framework.core.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.slf4j.Logger;

import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.IPipelineExtendersContributor;

public class CommonUtils {
    private static final char SEPARATOR_KEY_VALUE = '=';
	private static final String SEPARATOR_PROPERTIES = ";";
	
	private static List<IPipelineExtendersContributor> extendersContributors;

	public static boolean equalsEvenNull(Object obj1, Object obj2) {
		if (obj1 == null)
			return obj2 == null;
		
		return obj1.equals(obj2);
    }

	public static boolean equalsExceptNull(Object obj1, Object obj2) {
		if (obj1 == null || obj2 == null) {
			return false;
		}
		
		return obj1.equals(obj2);
	}
	
	public static Map<String, String> parsePropertiesString(String string, String[] acceptedStrings) {
		Map<String, String> properties = new HashMap<>();
		
		if (string == null || "".equals(string)) {
			return properties;
		}
		
		StringTokenizer tokenizer = new StringTokenizer(string, SEPARATOR_PROPERTIES);
		while (tokenizer.hasMoreTokens()) {
			String property = tokenizer.nextToken();
			int separatorIndex = property.indexOf(SEPARATOR_KEY_VALUE);
			if (property.length() < 3 || separatorIndex == -1 || separatorIndex == 0 ||
					separatorIndex == property.length() - 1) {
				throw new RuntimeException(String.format("Illegal property format: %s.", property));
			}
			
			String name = property.substring(0, separatorIndex).trim();
			checkPropertyName(name, acceptedStrings);
			String value = property.substring(separatorIndex + 1, property.length()).trim();
			
			properties.put(name, value);
		}
		
		return properties;
	}
	
	private static void checkPropertyName(String name, String[] acceptedStrings) {
		if (acceptedStrings != null && acceptedStrings.length != 0) {
			boolean accepted = false;
			for (String acceptedString : acceptedStrings) {
				if (name.equals(acceptedString)) {
					accepted = true;
					break;
				}
			}
			
			if (!accepted) {
				throw new IllegalArgumentException(String.format("Unknown property %s.", name));
			}
		}
	}
	
	public static String getInternalServerErrorMessage(RuntimeException e) {
		StringWriter sw = new StringWriter();
		BufferedWriter bw = new BufferedWriter(sw);
		PrintWriter pw = new PrintWriter(bw);
		e.printStackTrace(pw);
		
		try {
			bw.flush();
		} catch (IOException fe) {
			// ???
		}
		
		return sw.toString();
	}
	
	public static boolean isSetterMethod(Method method) {
		String methodName = method.getName();
		if (methodName.length() > 4 && methodName.startsWith("set") && Character.isUpperCase(methodName.charAt(3))) {
			Class<?>[] types = method.getParameterTypes();
			if (types.length != 1)
				return false;
			
			if (types[0].isPrimitive())
				return false;
			
			return true;
		}
		
		return false;
	}
	
	public static IPipelineExtendersContributor[] getExtendersContributors(IApplicationComponentService appComponentService) {
		if (extendersContributors == null) {
			extendersContributors = appComponentService.getPluginManager().getExtensions(IPipelineExtendersContributor.class);			
		}
		
		if (extendersContributors == null || extendersContributors.size() == 0)
			return new IPipelineExtendersContributor[0];
		
		for (IPipelineExtendersContributor extendersContributor : extendersContributors) {
			appComponentService.inject(extendersContributor);
		}
		
		return extendersContributors.toArray(new IPipelineExtendersContributor[extendersContributors.size()]);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T[] singletonArray(T singleton) {
		T[] array = (T[])(Array.newInstance(singleton.getClass(), 1));
		array[0] = singleton;
		
		return array;
	}
	
	public static void logAndThrow(Logger logger, RuntimeException exception) {
		logger.error("Unexpected exception occurred.", exception);
		throw exception;
	}
}
