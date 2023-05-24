package com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.config;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.granite.cluster.node.commons.utils.IoUtils;
import com.thefirstlineofcode.granite.cluster.node.commons.utils.SectionalProperties;

public class Config implements IConfig {
	private static final Logger logger = LoggerFactory.getLogger(Config.class);
	
	private Path configPath;
	private String content;
	private List<Property> properties;
	private Map<Integer, String> comments;
	private List<Section> sections;
	
	public Config() {
		this(null);
	}
	
	public Config(Path configPath) {
		this.configPath = configPath;
		properties = new ArrayList<>();
		comments = new HashMap<>();
		sections = new ArrayList<>();
	}
	
	@Override
	public void addOrUpdateProperty(String name, String value) {
		Property property = findProperty(name);
		if (property == null) {
			properties.add(new Property(name, value));
		} else {
			property.value = value;
		}
	}

	private Property findProperty(String name) {
		for (Property property : properties) {
			if (name.equals(property.name))
				return property;
		}
		
		return null;
	}

	@Override
	public String getProperty(String name) {
		for (Property property : properties) {
			if (name.equals(property.name))
				return property.value;
		}
		
		return null;
	}

	@Override
	public Set<String> getPropertyNames() {
		Set<String> propertyNames = new HashSet<>();
		
		for (Property property : properties) {
			propertyNames.add(property.name);
		}
		
		return propertyNames;
	}

	@Override
	public void addComment(String comment) {
		int position = properties.size();
		String existedComment = comments.get(position);
		if (existedComment != null) {
			comment = existedComment + "\r\n" + comment;
		}
		comments.put(position, comment);
	}

	@Override
	public IConfig getSection(String sectionName) {
		Section section = findSection(sectionName);
		
		if (section == null) {
			IConfig config = new Config() {
				@Override
				public IConfig getSection(String sectionName) {
					throw new UnsupportedOperationException("The config is already a sectional config.");
				}
			};
			
			section = new Section(sectionName, config);
			sections.add(section);
		}
		
		return section.config;
	}

	private Section findSection(String sectionName) {
		for (Section section : sections) {
			if (sectionName.equals(section.sectionName)) {
				return section;
			}
		}
		
		return null;
	}

	@Override
	public void setContent(String content) {
		this.content = content;
	}

	@Override
	public void save() {
		if (configPath == null)
			throw new RuntimeException("Not a savable config or null config path.");
		
		if (!properties.isEmpty() && !sections.isEmpty())
			throw new IllegalArgumentException("The config can be normal config or sectional config. But can't be both.");
		
		if (!sections.isEmpty()) {
			saveSectionalConfigToFile();
		} else if (!properties.isEmpty()) {
			saveNormalConfigToFile();
		} else if (content != null) {
			saveContentToFile();
		} else {
			try {
				// create an empty file.
				createConfigFile();
			} catch (Exception e) {
				throw new RuntimeException(String.format("Can't create configuration file %s.",
						configPath), e);
			}
			
			logger.warn("Null config. Config path is {}.", configPath);
		}
	}
	
	private void saveContentToFile() {
		try {
			IoUtils.writeToFile(content, configPath);
		} catch (IOException e) {
			throw new RuntimeException(String.format("Can't write config to %s.",
					configPath), e);
		}
	}

	private void saveNormalConfigToFile() {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(configPath.toFile()));
			
			int line = 0;
			for (Property property : properties) {
				String comment = comments.get(line);
				if (comment != null) {
					writeComment(writer, comment);
				}
				writeProperty(writer, property.name, property.value);
				line++;
			}
			
			String comment = comments.get(line);
			if (comment != null) {
				writeComment(writer, comment);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to write config to file.", e);
		} finally {
			IoUtils.close(writer);
		}
	}

	private Path createConfigFile() throws IOException {
		if (Files.exists(configPath)) {
			throw new RuntimeException(String.format("Can't save config to file %s. File has already exist.", configPath));
		}
		
		return Files.createFile(configPath);
	}

	private void writeProperty(BufferedWriter writer, String propertyName, String propertyValue) throws IOException {
		writer.write(String.format("%s=%s\r\n", propertyName, propertyValue));
	}

	private void writeComment(BufferedWriter writer, String comment) throws IOException {
		StringBuilder sb = new StringBuilder();
		char[] charArray = comment.toCharArray();
		for (int i = 0; i< charArray.length; i++) {
			if (i == 0) {
				sb.append('#');
			} else if (isNewLine(charArray, i)) {
				sb.append('#');
			}
			
			sb.append(charArray[i]);
		}
		
		sb.append("\r\n");
		
		writer.write(sb.toString());
	}

	private boolean isNewLine(char[] charArray, int pos) {
		return (charArray[pos] != '\r' && charArray[pos] != '\n') && (charArray[pos - 1] == '\n' || charArray[pos - 1] == '\r');
	}

	private void saveSectionalConfigToFile() {
		SectionalProperties sp = new SectionalProperties();
		for (Section section : sections) {
			Properties properties = new Properties();
			for (String propertyName : section.config.getPropertyNames()) {
				String propertyValue = section.config.getProperty(propertyName);
				properties.put(propertyName, propertyValue);
			}
			
			sp.addProperties(section.sectionName, properties);
		}
		
		try {
			if (Files.exists(configPath)) {
				Files.delete(configPath);
			}
			
			Files.createFile(configPath);
			
			sp.save(new BufferedOutputStream(new FileOutputStream(configPath.toFile())));
		} catch (Exception e) {
			throw new RuntimeException(String.format("Can't save config to file. Config file: %s", configPath), e);
		}
	}

	private class Property {
		public String name;
		public String value;
		
		public Property(String name, String value) {
			this.name = name;
			this.value = value;
		}
	}
		
	private class Section {
		public String sectionName;
		public IConfig config;
		
		public Section(String sectionName, IConfig config) {
			this.sectionName = sectionName;
			this.config = config;
		}
	}

	@Override
	public String getContent() {
		return content;
	}

	@Override
	public void addPropertyIfAbsent(String name, String value) {
		if (getPropertyNames().contains(name))
			return;
		
		properties.add(new Property(name, value));
	}

	@Override
	public void removeProperty(String name) {
		Property property = findProperty(name);
		
		if (property != null) {
			properties.remove(property);
		}
	}

	@Override
	public Path getConfigPath() {
		return configPath;
	}
	
}
