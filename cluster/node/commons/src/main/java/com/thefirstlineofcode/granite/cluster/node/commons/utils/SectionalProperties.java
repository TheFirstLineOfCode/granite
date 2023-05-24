package com.thefirstlineofcode.granite.cluster.node.commons.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SectionalProperties {
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	private List<Section> sections;
	
	public SectionalProperties() {
		sections = new ArrayList<>();
	}
	
	public void addProperties(String name, Properties properties) {
		sections.add(new Section(name, properties));
	}
	
	public String[] getSectionNames() {
		String[] sectionNames = new String[sections.size()];
		for (int i = 0; i < sections.size(); i++) {
			sectionNames[i] = sections.get(i).name;
		}
		
		return sectionNames;
	}
	
	public Properties getSection(String name) {
		for (Section section : sections) {
			if (section.name.equals(name))
				return section.properties;
		}
		
		return null;
	}
	
	public void load(InputStream inputStream) throws IOException {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(inputStream));
			
			String line = null;
			boolean lineContinuation = false;
			String sectionName = null;
			StringBuilder sectionContent = null;
			
			int currentLines = 0;
			while ((line = in.readLine()) != null) {
				currentLines++;
				line = line.trim();
				
				if (line.isEmpty()) {
					// ignore
					continue;
				}
				
				if (isSectionName(line)) {
					if (sectionName != null) {
						sections.add(new Section(sectionName, loadProperties(sectionContent.toString())));
					}
					
					sectionName = getSectionName(line);
					sectionContent = new StringBuilder();
					if (getSection(sectionName) != null)
						throw new IllegalArgumentException(String.format("Reduplicate section: %s.", sectionName));
					
					lineContinuation = false;
				} else if (isComment(line)) {
					// ignore
				} else if (isSectionContent(line) || lineContinuation) {
					if (sectionName == null) {
						throw new IllegalArgumentException(String.format("Illegal sectional properties file. Null section name. Illegal line is [line number: %d, line content: '%s'].", currentLines, line));
					}
					
					if (sectionContent.length() == 0) {
						sectionContent.append(line);
					} else {
						sectionContent.append(LINE_SEPARATOR).append(line);
					}
					
					if (isLineContinuation(line))
						lineContinuation = true;
					else
						lineContinuation = false;
				} else {
					throw new IllegalArgumentException(String.format("Illegal sectional properties file. Illegal line is [line number: %d, line content: '%s'].", currentLines, line));
				}
			}
			
			if (sectionName != null) {
				sections.add(new Section(sectionName, loadProperties(sectionContent.toString())));
			}
		} finally {
			IoUtils.close(in);
		}
	}

	private boolean isLineContinuation(String line) {
		return line.charAt(line.length() - 1) == '\\';
	}
	
	private Properties loadProperties(String content) {
		Properties properties = new Properties();
		try {
			properties.load(new  ByteArrayInputStream(content.getBytes()));
		} catch (IOException e) {
			throw new RuntimeException(String.format("Can't load properties. Section content is %s.", content), e);
		}
		
		return properties;
	}

	private boolean isComment(String line) {
		return line.startsWith("#");
	}
	
	private boolean isSectionContent(String line) {
		if (line.length() < 3)
			return false;
		
		int equalMarkIndex = line.indexOf('=');
		
		if (equalMarkIndex == -1 || equalMarkIndex == 0 || equalMarkIndex == line.length() - 1)
			return false;
		
		return true;
	}
	
	private String getSectionName(String line) {
		return line.substring(1, line.length() - 1);
	}
	
	private class Section {
		public String name;
		public Properties properties;
		
		public Section(String name, Properties properties) {
			this.name = name;
			this.properties = properties;
		}
	}
	
	private boolean isSectionName(String line) {
		return line.startsWith("[") && line.endsWith("]") && line.length() > 3;
	}
	
	public void save(OutputStream outputStream) throws IOException {
		StringBuilder sb = new StringBuilder();
		try {
			if (sections != null && !sections.isEmpty()) {
				for (int i = 0; i < sections.size(); i++) {
					Section section = sections.get(i);
					
					// Ignore empty section.
					if (section.properties.size() == 0)
						continue;
					
					sb.append(String.format("[%s]%s", section.name, LINE_SEPARATOR));
					
					for (String propertyName : section.properties.stringPropertyNames()) {
						sb.append(String.format("%s=%s%s", propertyName,
								section.properties.getProperty(propertyName),
								LINE_SEPARATOR));
					}
					
					if (i != sections.size() - 1)
						sb.append(LINE_SEPARATOR);
				}
				
				outputStream.write(sb.toString().getBytes());
				outputStream.flush();
			}
		} finally {
			IoUtils.close(outputStream);
		}
		
	}
}
