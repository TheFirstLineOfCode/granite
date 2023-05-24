package com.thefirstlineofcode.granite.cluster.node.commons.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
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
	
	public static String[] sort(String[] array) {
		if (array == null || array.length == 0)
			return new String[0];
		
		Arrays.sort(array, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				int lengthDifference = o1.length() - o2.length();
				
				if (lengthDifference != 0)
					return lengthDifference;
				
				for (int i = 0; i < o1.length(); i++) {
					if (o1.charAt(i) != o2.charAt(i)) {
						return o1.charAt(i) - o2.charAt(i);
					}
				}
				
				return 0;
			}
		});
		
		return array;
	}
	
	public static String getChecksum(String string) {	
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Can't get MD5 checksum.", e);
		}
		
		md.update(string.getBytes());
		byte[] digest = md.digest();

		return getHexStringFromBytes(digest);
	}
	
	private static String getHexStringFromBytes(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		
		for (byte b : bytes) {
			sb.append(String.format("0x%02x ", b & 0xff));
		}
		
		if (sb.length() > 1) {
			sb.deleteCharAt(sb.length() - 1);
		}
		
		return sb.toString();
	}
	
}
