package com.thefirstlineofcode.granite.pack.cluster.mgtnode;

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
	private static final String GRANITE_PROJECT_PACKAGE_PREFIX = "granite-";

	private static final String DIRECTORY_NAME_CACHE = ".cache";

	private static final String FILE_NAME_SUBSYSTEMS = "subsystems.ini";

	private static final String FILE_NAME_LIBRARYINFOS = "libraryinfos.ini";
	
	private Map<String, String[]> subsystems;
	private Map<String, LibraryInfo> libraryInfos;
	
	private Options options;
	
	private File repositoryDir;
	
	public Updater(Options options) {
		this.options = options;
		subsystems = new HashMap<>(6);
		libraryInfos = new HashMap<>(20);
	}

	public void cleanCache() {
		File cacheDir = new File(options.getTargetDirPath(), DIRECTORY_NAME_CACHE);
		if (cacheDir.exists()) {
			new File(cacheDir, FILE_NAME_LIBRARYINFOS).delete();
			new File(cacheDir, FILE_NAME_SUBSYSTEMS).delete();
			cacheDir.delete();
		}
	}
	
	public void update(boolean clean) {
		loadCache();
		
		String[] modules = options.getModules();
		if (modules == null)
			modules = getSubsystemNames();
		
		List<String> updatedLibraries = new ArrayList<>();
		for (String module : modules) {
			if (isSubsystem(module)) {
				updateSubsystem(module, clean, updatedLibraries);
			} else {
				if (!module.startsWith(GRANITE_PROJECT_PACKAGE_PREFIX)) {
					module = GRANITE_PROJECT_PACKAGE_PREFIX + module;
				}
				
				if (libraryInfos.containsKey(module)) {
					updateLibrary(module, clean, updatedLibraries);
				} else {
					System.out.println(String.format("Illegal library or subsystem: %s.", module));
					return;
				}
			}
		}
		
		StringBuilder libraries = new StringBuilder();
		for (String library : updatedLibraries) {
			libraries.append(library).append(", ");
		}
		
		if (libraries.length() > 0) {
			libraries.delete(libraries.length() - 2, libraries.length());
		}
		
		System.out.println(String.format("Libraries %s updated.", libraries.toString()));
	}

	private String[] getSubsystemNames() {
		return subsystems.keySet().toArray(new String[subsystems.size()]);
	}
	
	private void updateLibrary(String library, boolean clean, List<String> updatedLibraries) {
		LibraryInfo libraryInfo = libraryInfos.get(library);
		if (clean) {
			PackUtils.runMvn(new File(libraryInfo.developmentDir), options.isOffline(), "clean", "package");
		} else {
			PackUtils.runMvn(new File(libraryInfo.developmentDir), options.isOffline(), "package");
		}
		
		updateLibrary(libraryInfo);
		updatedLibraries.add(libraryInfo.fileName);
	}

	private void updateLibrary(LibraryInfo libraryInfo) {
		File targetDir = new File(libraryInfo.developmentDir, "target");
		File artifact = new File(targetDir, libraryInfo.fileName);
		if (!artifact.exists()) {
			throw new RuntimeException(String.format("Artifact %s doesn't exist.", artifact.getPath()));
		}
		
		if (isFileModified(libraryInfo.deployDir, artifact)) {
			File target = new File(libraryInfo.deployDir, artifact.getName());
			try {
				Files.copy(artifact.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.COPY_ATTRIBUTES);
			} catch (IOException e) {
				throw new RuntimeException(String.format("Can't copy file '%s' to '%s'.",
						artifact.getPath(), repositoryDir.getPath()), e);
			}
		}
	}

	private boolean isFileModified(String sDeployDir, File artifact) {
		File existing = new File(sDeployDir, artifact.getName());
		if (!existing.exists())
			return true;
		
		return existing.lastModified() != artifact.lastModified();
	}

	private void updateSubsystem(String subsystem, boolean clean, List<String> updatedLibraries) {
		String subsystemFullName = GRANITE_PROJECT_PACKAGE_PREFIX + subsystem;
		File subsystemProjectDir = new File(options.getGraniteProjectDirPath(), subsystemFullName);
		if (!subsystemProjectDir.exists()) {
			throw new RuntimeException(String.format("Subsystem[%s] project directory doesn't exist.", subsystem));
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
			reader = new BufferedReader(new FileReader(new File(cacheDir, FILE_NAME_LIBRARYINFOS)));
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
		libraryInfo.developmentDir = st.nextToken();
		libraryInfo.deployDir = st.nextToken();
		
		return libraryInfo;
	}

	private void createCache() {
		File cacheDir = new File(options.getTargetDirPath(), DIRECTORY_NAME_CACHE);
		if (!cacheDir.exists() && !cacheDir.mkdirs()) {
			throw new RuntimeException("Can't create cache directory.");
		}
		
		File repositoryDir = getRepositoryDir();
		
		for (File library : repositoryDir.listFiles()) {
			String libraryName = library.getName();
			if (!isGraniteArtifact(libraryName)) {
				continue;
			}
			
			LibraryInfo libraryInfo = new LibraryInfo();
			libraryInfo.fileName = libraryName;
			
			libraryInfos.put(getLibraryName(libraryName), libraryInfo);
		}
		
		collectCacheData(repositoryDir.getAbsolutePath());
		
		syncCacheToDisk(cacheDir);
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

	private File getRepositoryDir() {
		if (repositoryDir != null)
			return repositoryDir;
		
		if (options.getRepositoryDirPath() != null) {
			repositoryDir = new File(options.getRepositoryDirPath());
		}
		
		if (repositoryDir == null) {
			File appDir = new File(options.getTargetDirPath(), options.getAppName());
			if (!appDir.exists()) {
				throw new RuntimeException("App directory doesn't exist. Please extract zip file first.");
			}
			
			repositoryDir = new File(appDir, "repository");
		}
		
		if (!repositoryDir.exists()) {
			throw new RuntimeException(String.format("Repository directory %s doesn't exist.", repositoryDir.getPath()));
		}
		
		return repositoryDir;
	}

	private void syncCacheToDisk(File cacheDir) {
		syncLibraryInfosToDisk(cacheDir);
		syncSubsystemsToDisk(cacheDir);
	}

	private void syncLibraryInfosToDisk(File cacheDir) {
		Properties pLibraryInfos = new Properties();
		for (Map.Entry<String, LibraryInfo> entry : libraryInfos.entrySet()) {
			pLibraryInfos.put(entry.getKey(), libraryInfoToString(entry.getValue()));
		}
		
		Writer writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(cacheDir, FILE_NAME_LIBRARYINFOS)));
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

	private String libraryInfoToString(LibraryInfo libraryInfo) {
		StringBuilder sb = new StringBuilder();
		sb.append(libraryInfo.fileName).
			append(',').
			append(libraryInfo.developmentDir).
			append(',').
			append(libraryInfo.deployDir);
		
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

	private void collectCacheData(String repositoryDir) {
		libraryInfos.forEach((libraryName, libraryInfo) -> {
			StringTokenizer st = new StringTokenizer(libraryName, "-");
			int count = st.countTokens();
			if (count < 2) {
				throw new RuntimeException("This is an invalid granite library. Library name: " + libraryName);
			}
			
			// First token is system name. It must be "granite".
			st.nextToken();
			String rawSubsystemName = null;
			if (count > 2) {
				rawSubsystemName = st.nextToken();
			}
			
			String developmentDir = null;
			if (isGraniteArtifact(libraryName)) {
				developmentDir = PackUtils.getDevelopmentDir(options.getGraniteProjectDirPath(), libraryName);
			} else {
				throw new RuntimeException(String.format("Illegal granite library. %s isn't a system library or a plugin library.", libraryName));
			}
			
			if (!isLibraryDevProjectDir(new File((developmentDir))))
				throw new RuntimeException(String.format("%s isn't a library development project directory.", developmentDir));
			
			libraryInfo.developmentDir = developmentDir;
			libraryInfo.deployDir = repositoryDir;
			libraryInfos.put(libraryName, libraryInfo);
			
			if (rawSubsystemName != null) {
				String subsystem = GRANITE_PROJECT_PACKAGE_PREFIX + rawSubsystemName;
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

	private boolean isLibraryDevProjectDir(File dir) {
		if (!dir.exists())
			return false;
		
		if (!dir.isDirectory())
			return false;
		
		boolean pomFound = false;
		boolean srcFound = false;
		for (File file : dir.listFiles()) {
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

	private class LibraryInfo {
		public String fileName;
		public String developmentDir;
		public String deployDir;
	}

	private boolean isGraniteArtifact(String libraryFileName) {
		return libraryFileName.startsWith(GRANITE_PROJECT_PACKAGE_PREFIX);
	}

	private boolean isCacheCreated() {
		File cacheDir = new File(options.getTargetDirPath(), DIRECTORY_NAME_CACHE);
		if (!cacheDir.exists())
			return false;
		
		return new File(cacheDir, FILE_NAME_SUBSYSTEMS).exists() && new File(cacheDir, FILE_NAME_LIBRARYINFOS).exists();
	}
	
	private boolean isSubsystem(String module) {
		for (String subsystemName : getSubsystemNames()) {
			if (subsystemName.equals(module))
				return true;
		}
		
		return false;
	}
}
