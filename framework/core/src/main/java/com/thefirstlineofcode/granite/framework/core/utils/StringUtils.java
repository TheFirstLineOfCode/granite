package com.thefirstlineofcode.granite.framework.core.utils;

import java.util.StringTokenizer;

public class StringUtils {
	public static String[] stringToArray(String value) {
		if (value == null || value.isEmpty())
			return new String[0];
		
		StringTokenizer st = new StringTokenizer(value, ",");
		
		String[] array = new String[st.countTokens()];
		
		int i = 0;
		while (st.hasMoreTokens()) {
			array[i++] = st.nextToken().trim();
		}
		
		return array;
	}
	
	public static String arrayToString(String[] array) {
		if (array == null || array.length == 0)
			return "";
		
		StringBuilder sb = new StringBuilder();
		
		for (String string : array) {
			sb.append(string).append(',');
		}
		
		if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') {
			sb.deleteCharAt(sb.length() - 1);
		}
		
		return sb.toString();
	}

	public static boolean isEmpty(String string) {
		return string == null || string.isEmpty();
	}
	
}
