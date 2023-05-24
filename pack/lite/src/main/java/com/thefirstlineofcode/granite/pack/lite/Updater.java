package com.thefirstlineofcode.granite.pack.lite;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

public class Updater {
	private static final String DIRECTORY_NAME_LIBS = "libs";
	private static final String DIRECTORY_NAME_PLUGINS = "plugins";
	private static final String GRANITE_PROJECT_PREFIX = "granite-";
	private static final String SAND_PROJECT_PREFIX = "sand-";

	private static final String DIRECTORY_NAME_CACHE = ".cache";
	private static final String FILE_NAME_SUBSYSTEMS = "subsystems.ini";
	private static final String FILE_NAME_LIBRARIESINFOS = "libraryinfos.ini";
	
	private Map<String, String[]> subsystems;
	private Map<String, LibraryInfo> libraryInfos;
	
	private Options options;
	
	public Updater(Options options) {
		this.options = options;
		libraryInfos = new HashMap<>();
		subsystems = new HashMap<>();
	}

	public void cleanCache() {
		File cacheDir = new File(options.getTargetDirPath(), DIRECTORY_NAME_CACHE);
		if (cacheDir.exists()) {
			new File(cacheDir, FILE_NAME_LIBRARIESINFOS).delete();
			new File(cacheDir, FILE_NAME_SUBSYSTEMS).delete();
			cacheDir.delete();
		}
	}
	
	public void update(boolean clean) {
		loadCache();
		
		String[] modules = options.getModules();
		if (modules == null)
			modules = subsystems.keySet().toArray(new String[subsystems.size()]);
		
		List<String> updatedLibraries = new ArrayList<>();
		for (String module : modules) {
			if (isSubsystem(module)) {
				updateSubsystem(module, clean, updatedLibraries);
			} else {
				if (libraryInfos.containsKey(module)) {
					updateLibrary(module, clean, updatedLibraries);
				} else {
					System.out.println(String.format("Illegal subsystem or library name: %s.", module));
				}
			}
		}
		
		StringBuilder sbUpdatedLibraries = new StringBuilder();
		for (String library : updatedLibraries) {
			sbUpdatedLibraries.append(library).append(", ");
		}
		
		if (sbUpdatedLibraries.length() == 0) {
			System.out.println("No library updated.");
		} else {
			sbUpdatedLibraries.delete(sbUpdatedLibraries.length() - 2, sbUpdatedLibraries.length());
			System.out.println(String.format("Libraries %s updated.", sbUpdatedLibraries.toString()));
		}
	}
	
	private void updateLibrary(String library, boolean clean, List<String> updatedLibraries) {
		if (!libraryInfos.containsKey(library)) {
			throw new IllegalArgumentException(String.format("Illegal library name '%s'.", library));
		}
		
		LibraryInfo libraryInfo = libraryInfos.get(library);
		if (clean) {
			PackUtils.runMvn(new File(libraryInfo.developmentDir), options.isOffline(), "clean", "install");
		} else {
			PackUtils.runMvn(new File(libraryInfo.developmentDir), options.isOffline(), "install");
		}
		
		updateLibrary(libraryInfo);
		updatedLibraries.add(library);
	}

	private void updateLibrary(LibraryInfo libraryInfo) {
		if (isFileModified(libraryInfo)) {
			File newest = new File(libraryInfo.developmentDir + "/target", libraryInfo.fileName);
			File existing = new File(libraryInfo.deploymentDir, libraryInfo.fileName);
			try {
				Files.copy(newest.toPath(), existing.toPath(), StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.COPY_ATTRIBUTES);
			} catch (IOException e) {
				throw new RuntimeException(String.format("Can't copy file '%s' to '%s'.",
						newest.getName(), existing.toPath()), e);
			}
		}
	}

	private boolean isFileModified(LibraryInfo libraryInfo) {
		File existing = new File(libraryInfo.deploymentDir, libraryInfo.fileName);
		if (!existing.exists())
			return true;
		
		return getNewestLibrary(libraryInfo).lastModified() != existing.lastModified();
	}

	private File getNewestLibrary(LibraryInfo libraryInfo) {
		File targetDir = new File(libraryInfo.developmentDir, "target");
		File newestLibrary = new File(targetDir, libraryInfo.fileName);
		if (!newestLibrary.exists()) {
			throw new RuntimeException(String.format("Newest library '%s' doesn't exist. Please build it first.", newestLibrary.getPath()));
		}
		
		return newestLibrary;
	}

	private void updateSubsystem(String subsystem, boolean clean, List<String> updatedLibraries) {
		File subsystemProjectDir = null;
		
		if (subsystem.startsWith(GRANITE_PROJECT_PREFIX)) {
			subsystemProjectDir = new File(options.getGraniteProjectDirPath(), subsystem.substring(8));
		} else if (subsystem.startsWith(SAND_PROJECT_PREFIX)) {
			subsystemProjectDir = new File(options.getSandProjectDirPath(), subsystem.substring(5));
		} else {
			throw new IllegalArgumentException(String.format("Illegal subsystem name '%s'.", subsystem));
		}
		
		if (!subsystemProjectDir.exists()) {
			throw new RuntimeException(String.format("Subsystem[%s] project directory[%s] doesn't exist.", subsystem, subsystemProjectDir.getPath()));
		}
		
		if (clean) {
			PackUtils.runMvn(subsystemProjectDir, options.isOffline(), "clean", "package");
		} else {
			PackUtils.runMvn(subsystemProjectDir, options.isOffline(), "package");
		}
		
		String[] libraries = subsystems.get(subsystem);
		if (libraries == null)
			return;
		
		for (String library : libraries) {
			LibraryInfo libraryInfo = libraryInfos.get(library);
			
			if (libraryInfo == null) {
				throw new RuntimeException(String.format("Can't get library info for library %s.", library));
			}
			
			updateLibrary(libraryInfo);
			updatedLibraries.add(libraryInfo.fileName);
		}
	}

	private void loadCache() {
		if (!isCacheCreated()) {
			createCache();
		}
		
		loadCacheFromDisk();
	}

	private void loadCacheFromDisk() {
		loadLibraryInfosFromDisk();
		loadSubsystemsFromDisk();
	}

	private void loadSubsystemsFromDisk() {
		File cacheDir = new File(options.getTargetDirPath(), DIRECTORY_NAME_CACHE);
		
		Properties pSubsystems = new Properties();
		
		Reader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(new File(cacheDir, FILE_NAME_SUBSYSTEMS)));
			pSubsystems.load(reader);
		} catch (IOException e) {
			throw new RuntimeException("Can't load cache from disk.", e);
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					// ignore
				}
		}
		
		subsystems = new HashMap<>();
		for (Map.Entry<Object, Object> entry : pSubsystems.entrySet()) {
			String subsystemsName = (String)entry.getKey();
			String[] libraries = stringToArray((String)entry.getValue());
			
			subsystems.put(subsystemsName, libraries);
		}
	}
	
	private String[] stringToArray(String string) {
		StringTokenizer st = new StringTokenizer(string, ",");
		int count = st.countTokens();
		String[] array = new String[count];
		
		for (int i = 0; i < count; i++) {
			array[i] = st.nextToken();
		}
		
		return array;
	}

	private void loadLibraryInfosFromDisk() {
		File cacheDir = new File(options.getTargetDirPath(), DIRECTORY_NAME_CACHE);
		
		Properties pLibraryInfos = new Properties();
		
		Reader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(new File(cacheDir, FILE_NAME_LIBRARIESINFOS)));
			pLibraryInfos.load(reader);
		} catch (IOException e) {
			throw new RuntimeException("Can't load cache from disk.", e);
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					// ignore
				}
		}
		
		libraryInfos = new HashMap<>();
		for (Map.Entry<Object, Object> entry : pLibraryInfos.entrySet()) {
			String libraryName = (String)entry.getKey();
			LibraryInfo libraryInfo = stringToLibraryInfo((String)entry.getValue());
			
			libraryInfos.put(libraryName, libraryInfo);
		}
	}

	private LibraryInfo stringToLibraryInfo(String string) {
		StringTokenizer st = new StringTokenizer(string, ",");
		if (st.countTokens() != 3) {
			throw new RuntimeException("Bad cache format[Library Info].");
		}
		
		LibraryInfo libraryInfo = new LibraryInfo();
		libraryInfo.fileName = st.nextToken();
		libraryInfo.deploymentDir = st.nextToken();
		libraryInfo.developmentDir = st.nextToken();
		
		return libraryInfo;
	}

	private void createCache() {
		File cacheDir = new File(options.getTargetDirPath(), DIRECTORY_NAME_CACHE);
		if (!cacheDir.exists() && !cacheDir.mkdirs()) {
			throw new RuntimeException("Can't create cache directory.");
		}
		
		findSystemLibraries();
		findPluginLibraries();
		collectCacheData();
		
		syncCacheToDisk(cacheDir);
	}

	private void findSystemLibraries() {
		findLibrariesFromDirectory(new File(String.format("%s/%s", options.getTargetDirPath(), options.getAppName()), DIRECTORY_NAME_LIBS));
	}
	
	private void findPluginLibraries() {
		findLibrariesFromDirectory(new File(String.format("%s/%s", options.getTargetDirPath(), options.getAppName()), DIRECTORY_NAME_PLUGINS));
	}

	private void findLibrariesFromDirectory(File librariesDir) {
		if (!librariesDir.exists()) {
			throw new IllegalArgumentException(String.format("Libraries directory '%s' not existed.", librariesDir));
		}
		
		if (!librariesDir.isDirectory())
			throw new RuntimeException(String.format("Libraries directory %s isn't a directory.", librariesDir.getAbsolutePath()));
		
		for (File file : librariesDir.listFiles()) {
			String fileName = file.getName();
			if (!isGraniteLibraryFile(fileName) && !isSandLibraryFile(fileName)) {
				continue;
			}
			
			LibraryInfo libraryInfo = new LibraryInfo();
			libraryInfo.fileName = fileName;
			libraryInfo.deploymentDir = librariesDir.getAbsolutePath();
			
			libraryInfos.put(getLibraryName(fileName), libraryInfo);
		}

	}
	
	private String getLibraryName(String fileName) {
		String libraryFullName = fileName.substring(0, fileName.length() - 4);
		
		int lastDashIndex = libraryFullName.lastIndexOf('-');
		if (lastDashIndex == -1)
			return libraryFullName;
		
		String lastVersionIdentifier = libraryFullName.substring(lastDashIndex + 1, libraryFullName.length());
		String libraryName = libraryFullName.substring(0, lastDashIndex);
		if (!isVersionCore(lastVersionIdentifier)) {
			lastDashIndex = libraryName.lastIndexOf('-');
			if (lastDashIndex == -1)
				return libraryName;
			
			libraryName = libraryName.substring(0, lastDashIndex);
		}
		
		return libraryName;
	}

	private boolean isVersionCore(String versionIdentifier) {
		StringTokenizer st = new StringTokenizer(versionIdentifier, ".");
		while (st.hasMoreTokens()) {
			String nextLevelVersion = st.nextToken();
			try {				
				Integer.parseInt(nextLevelVersion);
			} catch (NumberFormatException e) {
				return false;
			}
		}
		
		return true;
	}

	private void syncCacheToDisk(File cacheDir) {
		syncLibraryInfosToDisk(cacheDir);
		syncSubsystemsToDisk(cacheDir);
	}

	private void syncLibraryInfosToDisk(File cacheDir) {
		Properties pLibraryInfos = new Properties();
		for (Map.Entry<String, LibraryInfo> entry : libraryInfos.entrySet()) {
			pLibraryInfos.put(entry.getKey(), convertLibraryInfoToString(entry.getValue()));
		}
		
		Writer writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(cacheDir, FILE_NAME_LIBRARIESINFOS)));
			pLibraryInfos.store(writer, null);
		} catch (IOException e) {
			throw new RuntimeException("Can't sync cache to disk.", e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	private Object convertLibraryInfoToString(LibraryInfo libraryInfo) {
		StringBuilder sb = new StringBuilder();
		sb.append(libraryInfo.fileName).
			append(',').
			append(libraryInfo.deploymentDir).
			append(',').
			append(libraryInfo.developmentDir);
		
		return sb.toString();
	}

	private void syncSubsystemsToDisk(File cacheDir) {
		Properties pSubsystems = new Properties();
		for (Map.Entry<String, String[]> entry : subsystems.entrySet()) {
			pSubsystems.put(entry.getKey(), convertArrayToString(entry.getValue()));
		}
		
		Writer writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(cacheDir, FILE_NAME_SUBSYSTEMS)));
			pSubsystems.store(writer, null);
		} catch (IOException e) {
			throw new RuntimeException("Can't sync cache to disk.", e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	private String convertArrayToString(String[] array) {
		StringBuilder sb = new StringBuilder();
		
		for (String string : array) {
			sb.append(string).append(',');
		}
		
		sb.deleteCharAt(sb.length() - 1); // delete last comma
		
		return sb.toString();	
	}

	private void collectCacheData() {
		String graniteProjectPath = new File(options.getGraniteProjectDirPath()).getAbsolutePath();
		
		String tmpSandProjectPath = null;
		if (options.getSandProjectDirPath() != null) {
			tmpSandProjectPath = new File(options.getSandProjectDirPath()).getAbsolutePath();
		}
		final String sandProjectPath = tmpSandProjectPath;
		
		libraryInfos.forEach((libraryName, libraryInfo) -> {
			StringTokenizer st = new StringTokenizer(libraryName, "-");
			int count = st.countTokens();
			if (count < 2) {
				throw new RuntimeException("It's an invalid lithosphere library. Library name: " + libraryName);
			}
			
			// First token is system name. It's "granite" or "sand".
			st.nextToken();
			String rawSubsystemName = null;
			if (count > 2) {
				rawSubsystemName = st.nextToken();
			}
			
			String developmentDir = null;
			if (isGraniteLibrary(libraryName)) {
				developmentDir = PackUtils.getDevelopmentDir(graniteProjectPath, libraryName);
			} else if (isSandLibrary(libraryName)) {
				if (options.getSandProjectDirPath() == null)
					throw new RuntimeException("Can't determine sand project root directoy.");
				
				developmentDir = PackUtils.getDevelopmentDir(sandProjectPath, libraryName);
			} else {
				throw new RuntimeException(String.format("Illegal granite library. %s isn't a system library or a plugin library.", libraryName));
			}
			
			libraryInfo.developmentDir = developmentDir;
			libraryInfos.put(libraryName, libraryInfo);
			
			if (rawSubsystemName != null) {
				String subsystem;
				if (isGraniteLibrary(libraryName)) {
					subsystem = GRANITE_PROJECT_PREFIX + rawSubsystemName;
				} else {
					subsystem = SAND_PROJECT_PREFIX + rawSubsystemName;					
				}
				
				String[] libraries = subsystems.get(subsystem);
				if (libraries == null) {
					subsystems.put(subsystem, new String[] {libraryName});
				} else {
					 String[] newLibraries = Arrays.copyOf(libraries, libraries.length + 1);
					 newLibraries[newLibraries.length - 1] = libraryName;
					 subsystems.put(subsystem, newLibraries);
				}
			}
		});
	}
	
	private class LibraryInfo {
		public String deploymentDir;
		public String fileName;
		public String developmentDir;
	}

	private boolean isGraniteLibrary(String libraryName) {
		return libraryName.startsWith(GRANITE_PROJECT_PREFIX);
	}
	
	private boolean isSandLibrary(String libraryName) {
		if (options.getSandProjectName() == null)
			return false;
		
		return libraryName.startsWith(SAND_PROJECT_PREFIX);
	}
	
	private boolean isGraniteLibraryFile(String libraryFileName) {
		return libraryFileName.startsWith(GRANITE_PROJECT_PREFIX) && libraryFileName.endsWith(".jar");
	}
	
	private boolean isSandLibraryFile(String libraryFileName) {
		return libraryFileName.startsWith(SAND_PROJECT_PREFIX) && libraryFileName.endsWith(".jar");
	}
	

	private boolean isCacheCreated() {
		File cacheDir = new File(options.getTargetDirPath(), DIRECTORY_NAME_CACHE);
		if (!cacheDir.exists())
			return false;
		
		return new File(cacheDir, FILE_NAME_SUBSYSTEMS).exists() && new File(cacheDir, FILE_NAME_LIBRARIESINFOS).exists();
	}
	
	private boolean isSubsystem(String module) {
		for (String subsystemName : subsystems.keySet()) {
			if (subsystemName.equals(module))
				return true;
		}
		
		return false;
	}
}
