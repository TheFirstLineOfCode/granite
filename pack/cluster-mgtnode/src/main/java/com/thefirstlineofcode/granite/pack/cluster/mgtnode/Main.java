package com.thefirstlineofcode.granite.pack.cluster.mgtnode;

import java.util.Arrays;

public class Main {
	private static final String DEFAULT_VERSION = "0.2.1.RELEASE";
	private static final String NAME_PREFIX_APP = "granite-cluster-node-mgtnode-";
	
	public static void main(String[] args) {
		Main main = new Main();
		main.run(args);
	}
	
	public void run(String[] args) {
		Options options = null;
		try {
			options = parseOptions(args);
		} catch (IllegalArgumentException e) {
			if (e.getMessage() != null)
				System.out.println("Error: " + e.getMessage());
			printUsage();
			return;
		}
		
		if (options.isHelp()) {
			printUsage();
			return;
		}
		
		if (options.isPack()) {
			new Packer(options).pack();
		} else {
			Updater updater = new Updater(options);
			
			if (options.isCleanCache()) {
				updater.cleanCache();
			}
			
			if (options.isCleanUpdate()) {
				updater.update(true);
			} else {
				updater.update(false);
			}
		}
	}
	
	private Options parseOptions(String[] args) {
		Options options = new Options();
		
		if (args.length == 1 && args[0].equals("-help")) {
			options.setHelp(true);
			
			return options;
		}
		
		int i = 0;
		while (i < args.length) {
			if ("-update".equals(args[i])) {
				options.setUpdate(true);
				i++;
			} else if ("-cleanUpdate".equals(args[i])) {
				options.setCleanUpdate(true);
				i++;
			} else if ("-cleanCache".equals(args[i])) {
				options.setCleanCache(true);
				i++;
			} else if ("-version".equals(args[i])) {
				if (i == (args.length - 1)) {
					throw new IllegalArgumentException("-version should follow a [VERSION] option value.");
				}
				i++;
				
				if (args[i].startsWith("-")) {
					throw new IllegalArgumentException("-version should follow a [VERSION] option value.");
				}
				
				options.setVersion(args[i]);
				i++;
			} else if ("-repositoryDir".equals(args[i])) {
				if (i == (args.length - 1)) {
					throw new IllegalArgumentException("-repositorDir should follow a [REPOSITORY_DIR] option value.");
				}
				i++;
				
				if (args[i].startsWith("-")) {
					throw new IllegalArgumentException("-repositoryDir should follow a [REPOSITORY_DIR] option value.");
				}
				
				options.setRepositoryDirPath(args[i]);
				i++;
			} else if ("-offline".equals(args[i])) {
				options.setOffline(true);
				i++;
			} else if ("-commerical".equals(args[i])) {
				options.setCommerical(true);
				i++;
			} else if ("-help".equals(args[i])) {
				throw new IllegalArgumentException("-help should be used alonely.");
			} else {
				options.setModules(Arrays.copyOfRange(args, i, args.length));
				break;
			}
		}
		
		if (options.isUpdate() && options.isCleanUpdate()) {
			throw new IllegalArgumentException("You can specify option -update or -cleanUpdate but not both.");
		}
		
		if (!options.isUpdate() && !options.isCleanUpdate() && options.getModules() != null) {
			throw new IllegalArgumentException("[BUNDLE_SYMBOLIC_NAME]... is only used in update mode. Maybe you should add -update or -clean-update to options.");
		}
		
		if (options.getVersion() == null) {
			options.setVersion(DEFAULT_VERSION);
		}
		
		options.setAppName(NAME_PREFIX_APP + options.getVersion());
		
		options.setTargetDirPath(PackUtils.getTargetDirPath(this));
		options.setProjectDirPath(PackUtils.getProjectDirPath(options.getTargetDirPath()));
		options.setGraniteProjectDirPath(PackUtils.getGraniteProjectDirPath(options.getProjectDirPath()));
		
		return options;
	}
	
	private void printUsage() {
		System.out.println("Usage:");
		System.out.println("java -jar granite-pack-cluster-mgtnode-${VERSION}.jar [OPTIONS] [Bundle-SymbolicNames or SubSystems]");
		System.out.println("OPTIONS:");
		System.out.println("-help                            Display help information.");
		System.out.println("-update                          Update specified modules.");
		System.out.println("-cleanUpdate                     Clean and update specified modules.");
		System.out.println("-cleanCache                      Clean the packing cache.");
		System.out.println("-repositoryDir REPOSITORY_DIR    Specify the path of repository directory.");
		System.out.println("-offline                         Run in offline mode.");
		System.out.println("-version VERSION                 Specify the version(Default is 0.2.1-RELEASE).");
	}
}
