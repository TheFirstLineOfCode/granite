package com.thefirstlineofcode.granite.pack.lite;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URL;

public abstract class PackUtils {
	public static void runMvn(File workingDir, boolean offline, String... args) {
		if (args == null || args.length == 0) {
			throw new IllegalArgumentException("Null mvn args.");
		}
		
		String[] cmdArray;
		if (offline) {
			cmdArray = new String[args.length + 2];			
			
			cmdArray[0] = getMvnCmd();
			cmdArray[1] = "-o";
			for (int i = 0; i < args.length; i++) {
				cmdArray[i + 2] = args[i];
			}
		} else {
			cmdArray = new String[args.length + 1];
			
			cmdArray[0] = getMvnCmd();
			for (int i = 0; i < args.length; i++) {
				cmdArray[i + 1] = args[i];
			}
		}
		
		try {
			Process process = new ProcessBuilder(cmdArray).
						redirectError(Redirect.INHERIT).
						redirectOutput(Redirect.INHERIT).
						directory(workingDir).
						start();
			
			process.waitFor();
		} catch (IOException e) {
			throw new RuntimeException("Can't execute maven.", e);
		} catch (InterruptedException e) {
			throw new RuntimeException("Maven execution error.", e);
		}
	}
	
	private static String getMvnCmd() {
		String osName = System.getProperty("os.name");
		if (osName.contains("Windows")) {
			return "mvn.cmd";
		}
		
		return "mvn";
	}
	
	public static String getTargetDirPath(Object runningObject) {
		URL classPathRoot = runningObject.getClass().getResource("");
		
		if (classPathRoot.getPath().indexOf("!") != -1) {
			int colonIndex =  classPathRoot.getFile().indexOf('!');
			String jarPath =  classPathRoot.getPath().substring(0, colonIndex);
			
			int lastSlashIndex = jarPath.lastIndexOf('/');
			String jarParentDirPath = jarPath.substring(5, lastSlashIndex);
			
			return jarParentDirPath;
		} else {
			int classesIndex = classPathRoot.getPath().lastIndexOf("/classes");	
			return classPathRoot.getPath().substring(0, classesIndex);
		}
	}
	
	public static String getGraniteProjectDirPath(String projectDirPath) {
		return new File(projectDirPath).getParentFile().getParentFile().getPath();
	}
	
	public static String getSandProjectDirPath(String projectDirPath, String sandProjectName) {
		File lithosphereDir = new File(projectDirPath).getParentFile().getParentFile().getParentFile();
		
		File sandProjectDir = new File(lithosphereDir, sandProjectName);
		if (sandProjectDir.exists()) {
			return sandProjectDir.getPath();
		}
		
		return null;
	}
	
	public static String getProjectDirPath(String targetDirPath) {
		return new File(targetDirPath).getParentFile().getPath();
	}
	
	public static String getDevelopmentDir(String rootProjectPath, String libraryName) {
		File currentDir = new File(rootProjectPath);
		String projectDirName = libraryName;
		int firstDashIndex = projectDirName.indexOf('-');
		projectDirName = libraryName.substring(firstDashIndex + 1);
		
		while (true) {
			if (isLibraryDevProjectDir(currentDir, projectDirName, libraryName))
				return new File(currentDir, projectDirName).getAbsolutePath();
			
			firstDashIndex = projectDirName.indexOf('-');
			if (firstDashIndex == -1) {
				throw new IllegalArgumentException(String.format("Can't find development directory for library: %s.", libraryName));
			}
			
			String childDirName = projectDirName.substring(0, firstDashIndex);
			currentDir = new File(currentDir, childDirName);	
			projectDirName = projectDirName.substring(firstDashIndex + 1);
		}
	}
	
	private static boolean isLibraryDevProjectDir(File parentDir, String projectDirName, String libraryName) {
		if (!parentDir.exists() || !parentDir.isDirectory())
			throw new IllegalArgumentException(String.format("Can't find development directory for library: %s.", libraryName));			
		File developmentDir = new File(parentDir, projectDirName);
		if (!developmentDir.exists())
			return false;
		
		if (!developmentDir.isDirectory())
			return false;
		
		boolean pomFound = false;
		boolean srcFound = false;
		for (File file : developmentDir.listFiles()) {
			if (file.getName().equals("src") && file.isDirectory()) {
				srcFound = true;
			} else if (file.getName().equals("pom.xml")) {
				pomFound = true;
			} else {
				continue;
			}
			
			if (srcFound && pomFound) {
				break;
			}
		}
		
		return srcFound && pomFound;
	}
}
