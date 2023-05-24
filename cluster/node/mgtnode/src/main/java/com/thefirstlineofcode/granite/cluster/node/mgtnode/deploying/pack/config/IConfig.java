package com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.config;

import java.nio.file.Path;
import java.util.Set;

public interface IConfig {
	Path getConfigPath();
	void setContent(String content);
	String getContent();
	void addOrUpdateProperty(String name, String value);
	void addPropertyIfAbsent(String name, String value);
	String getProperty(String name);
	Set<String> getPropertyNames();
	void removeProperty(String name);
	void addComment(String comment);
	IConfig getSection(String sectionName);
	void save();
}
