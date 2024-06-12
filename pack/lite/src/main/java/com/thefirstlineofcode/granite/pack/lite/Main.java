package com.thefirstlineofcode.granite.pack.lite;

import java.util.Arrays;

import com.thefirstlineofcode.granite.pack.lite.Options.Protocol;
import com.thefirstlineofcode.granite.pack.lite.Options.WebcamMode;

public class Main {
	private static final String DEFAULT_VERSION = "1.0.5-RELEASE";
	private static final String NAME_PREFIX_APP = "granite-lite";
	private static final String DEFAULT_SAND_PROJECT_NAME = "sand";
	
	private Options options;
	
	public static void main(String[] args) {
		Main main = new Main();
		main.run(args);
	}
	
	public void run(String[] args) {
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
			}/* else if ("-version".equals(args[i])) {
				if (i == (args.length - 1)) {
					throw new IllegalArgumentException("-version should follow a <VERSION> option value.");
				}
				i++;
				
				if (args[i].startsWith("-")) {
					throw new IllegalArgumentException("-version should follow a <VERSION> option value.");
				}
				
				options.setVersion(args[i]);
				i++;
			} */else if("-protocol".equals(args[i])) {
				if (i == (args.length - 1)) {
					throw new IllegalArgumentException("-protocol should follow a <PROTOCOL> option value.");
				}
				i++;
				
				if (args[i].startsWith("-")) {
					throw new IllegalArgumentException("-protocol should follow a <PROTOCOL> option value.");
				}
				
				if ("mini".equals(args[i])) {
					options.setProtocol(Options.Protocol.MINI);
				} else if ("standard".equals(args[i])) {
					options.setProtocol(Options.Protocol.STANDARD);
				} else if ("iot".equals(args[i])) {
					options.setProtocol(Options.Protocol.IOT);
				} else if ("sand-demo".equals(args[i])) {
					options.setProtocol(Protocol.SAND_DEMO);
				} else {
					throw new IllegalArgumentException(String.format("Illegal protocol: %s. Only 'mini', 'standard', 'iot' and 'sand-demo' are supported.", args[i]));
				}
				i++;
			} else if ("-webcamMode".equals(args[i])) {
				if (i == (args.length - 1)) {
					throw new IllegalArgumentException("-webcamMode should follow a <WEBCAM-MODE> option value.");
				}
				i++;
				
				if (args[i].startsWith("-")) {
					throw new IllegalArgumentException("-webcamMode should follow a <WEBCAM-MODE> option value.");
				}
				
				if ("none".equals(args[i])) {
					options.setWebcamMode(WebcamMode.NONE);
				} else if ("p2p".equals(args[i])) {
					options.setWebcamMode(WebcamMode.P2P);
				} else if ("kurento".equals(args[i])) {
					options.setWebcamMode(WebcamMode.KURENTO);
				} else {
					throw new IllegalArgumentException(String.format("Illegal webcam mode: %s. Only 'none', 'p2p' and 'kurento' are supported.", args[i]));
				}
				i++;
			} else if ("-commerical".equals(args[i])) {
				options.setCommerical(true);
				i++;
			} else if ("-offline".equals(args[i])) {
				options.setOffline(true);
				i++;
			} else {
				options.setModules(Arrays.copyOfRange(args, i, args.length));
				break;
			}
		}
		
		if (options.isUpdate() && options.isCleanUpdate()) {
			throw new IllegalArgumentException("You can specify option -update or -cleanUpdate. But not both.");
		}
		
		if (!options.isUpdate() && !options.isCleanUpdate() && options.getModules() != null) {
			throw new IllegalArgumentException("[BUNDLE_SYMBOLIC_NAME]... is only used in update mode. Maybe you should add -update or -clean-update to options.");
		}
		
		if (options.getVersion() == null) {
			options.setVersion(DEFAULT_VERSION);
		}
		
		if (options.getWebcamMode() == null) {			
			if (options.getProtocol() == Protocol.SAND_DEMO) {
				options.setWebcamMode(WebcamMode.P2P);
			} else {
				options.setWebcamMode(WebcamMode.NONE);
			}
		}
		
		options.setAppName(String.format("%s-%s-%s", NAME_PREFIX_APP, options.getProtocol().toString().toLowerCase(), options.getVersion()));
		
		if (!options.isUpdate() && !options.isCleanUpdate() && options.getProtocol() == null) {
			options.setProtocol(Options.Protocol.STANDARD);
		}
		
		options.setTargetDirPath(PackUtils.getTargetDirPath(this));
		options.setProjectDirPath(PackUtils.getProjectDirPath(options.getTargetDirPath()));
		options.setGraniteProjectDirPath(PackUtils.getGraniteProjectDirPath(options.getProjectDirPath()));
		
		options.setSandProjectDirPath(PackUtils.getSandProjectDirPath(options.getProjectDirPath(), DEFAULT_SAND_PROJECT_NAME));
		
		return options;
	}
	
	private void printUsage() {
		System.out.println("Usage:");
		System.out.println("java -jar granite-pack-lite-${VERSION}.jar [OPTIONS] [Bundle-SymbolicNames or SubSystems]");
		System.out.println("OPTIONS:");
		System.out.println("-help                                  Display help information.");
		System.out.println("-update                                Update specified modules.");
		System.out.println("-cleanUpdate                           Clean and update specified modules.");
		System.out.println("-cleanCache                            Clean the packing cache.");
		System.out.println("-offline                               Run in offline mode.");
		System.out.println("-protocol <PROTOCOL>                   Specify the protocol. Optional protocols are 'mini', 'standard', 'iot' or 'sand-demo'. Default is 'standard'.");
		System.out.println("-webcamMode <WEBCAM-MODE>              Specify that which webcam plugin needs to be deployed. Optional plugins are 'none', 'p2p', 'kurento'. Default is 'none'.");
	}
}
