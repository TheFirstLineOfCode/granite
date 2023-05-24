package com.thefirstlineofcode.granite.cluster.node.mgtnode.deploying.pack.config;

import java.nio.file.Path;

public interface IConfigManager {
	IConfig createOrGetConfig(Path parentPath, String configFileName);
	void saveConfigs();
}
