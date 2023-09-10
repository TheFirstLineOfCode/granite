package com.thefirstlineofcode.granite.cluster.node.commons.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeUtils {
	private static final Logger logger = LoggerFactory.getLogger(NodeUtils.class);
	
	private static final String FILE_SEPARATOR = System.getProperty("file.separator");

	public static void close(InputStream in) {
		if (in != null) {
			try {
				in.close();
			} catch (Exception e) {
				logger.trace("Failed to close the stream.", e);
			}
		}
	}

	public static void close(OutputStream out) {
		if (out != null) {
			try {
				out.close();
			} catch (Exception e) {
				 logger.trace("Failed to close the stream.", e);
			}
		}
	}

	public static boolean deleteFileRecursively(File file) {
		if (!file.isDirectory()) {
			return file.delete();
		} else {
			for (File subFile : file.listFiles()) {
				if (!deleteFileRecursively(subFile)) {
					return false;
				}
			}

			return file.delete();
		}
	}

	public static void close(Writer writer) {
		if (writer != null) {
			try {
				writer.close();
			} catch (Exception e) {
				logger.warn("Failed to close the writer.", e);
			}
		}
	}

	public static void close(Reader reader) {
		if (reader != null) {
			try {
				reader.close();
			} catch (Exception e) {
				logger.warn("Failed to close the reader.", e);
			}
		}
	}

	public static void writeToFile(String content, Path path) throws IOException {
		if (!Files.exists(path)) {
			Files.createFile(path);
		}
		
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(path.toFile()));
			out.write(content);
		} finally {
			NodeUtils.close(out);
		}
	}

    public static void writeToFile(InputStream in, Path path) throws IOException {
		if (!Files.exists(path)) {
			Files.createFile(path);
		}
		
		BufferedInputStream BufferedIn = null;
    	BufferedOutputStream out = null;
		try {
			BufferedIn = new BufferedInputStream(in);
			out = new BufferedOutputStream(new FileOutputStream(path.toFile()));
			
			byte[] buf = new byte[2048];
			int size = -1;
			while ((size = BufferedIn.read(buf)) != -1) {
				out.write(buf, 0, size);
			}
			
		} finally {
			NodeUtils.close(BufferedIn);
			NodeUtils.close(out);
		}
    }
	
	public static String readFile(Path path) throws IOException {
		BufferedReader in = null;
		
		try {
			in = new BufferedReader(new FileReader(path.toFile()));
			
			char[] buf = new char[1024];
			int size = -1;
			StringBuilder sb = new StringBuilder();
			while ((size = in.read(buf, 0, buf.length)) != -1) {
				sb.append(buf, 0, size);
			}
			
			return sb.toString();
		} catch (IOException e) {
			throw e;
		} finally {
			NodeUtils.close(in);
		}
	}
	
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
			sb.append(String.format("0x%02x", b & 0xff));
		}
		
		if (sb.length() > 1) {
			sb.deleteCharAt(sb.length() - 1);
		}
		
		return sb.toString();
	}
	
	public static void zip(File source, File target) throws TargetExistsException, IOException {
		if (!source.isDirectory()) {
			throw new IllegalArgumentException(String.format("Not support zip a folder. %s isn't a folder.", source.getPath()));
		}
		
		zipFolder(source, target);
	}
	
	public static void zipFolder(File sourceFolder, File targetFile) throws TargetExistsException, IOException {
		if (!sourceFolder.exists()) {
			throw new IllegalArgumentException(String.format("Source folder[%s] to zip doesn't exist."));
		}
		
		if (!sourceFolder.isDirectory()) {
			throw new IllegalArgumentException("Source folder[%s] isn't a folder.");
		}
		
		if (targetFile.exists()) {
			throw new TargetExistsException(String.format("Zip file[%s] has already existed.", targetFile.getPath()));
		}
		
		File[] files = getAllAescendantFiles(sourceFolder);
		ZipOutputStream zos = null;
		try {
			zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(targetFile)));
			
			for (File file : files) {
				writeFileToZip(zos, getEntryPath(sourceFolder, file), file);
			}
		} finally {
			NodeUtils.close(zos);
		}
	}
	
	private static File[] getAllAescendantFiles(File sourceFolder) {
		List<File> aescendants = new ArrayList<>();
		
		getAllAescendantFiles(sourceFolder, aescendants);
		
		return aescendants.toArray(new File[aescendants.size()]);
	}

	private static void getAllAescendantFiles(File folder, List<File> aescendants) {
		File[] children = folder.listFiles();
		for (File child : children) {
			if (child.isDirectory()) {
				getAllAescendantFiles(child, aescendants);
			} else {
				aescendants.add(child);
			}
		}
	}

	private static String getEntryPath(File sourceFolder, File file) {
		if (!file.getPath().startsWith(sourceFolder.getPath())) {
			return file.getPath();
		}
		
		String entryPath = file.getPath();
		entryPath = entryPath.substring(sourceFolder.getPath().length(), entryPath.length());
		
		
		if (entryPath.startsWith(FILE_SEPARATOR)) {
			entryPath = entryPath.substring(FILE_SEPARATOR.length(), entryPath.length());
		}
		
		return entryPath;
	}

	private static void writeFileToZip(ZipOutputStream zos, String entryPath, File file) throws IOException {
		zos.putNextEntry(new ZipEntry(entryPath));
		
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
		try {
			byte[] buf = new byte[2048];
			
			int size = -1;
			while ((size = bis.read(buf)) != -1) {
				zos.write(buf, 0, size);
			}
		} finally {
			bis.close();
		}
		
		zos.closeEntry();
	}
	
	public static void unzip(File sourceFile) throws IOException {
		unzip(sourceFile, sourceFile.getParentFile());
	}
	
	public static void unzip(File sourceFile, File targetFolder) throws IOException {
		if (sourceFile.isDirectory()) {
			throw new IllegalArgumentException(String.format("%s is a directory.", sourceFile));
		}
		
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(sourceFile);
			
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				
				if (entry.isDirectory()) {
					// ignore
					continue;
				}
				
				File file = getFile(targetFolder, entry.getName());
				if (!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				
				if (file.exists()) {
					Files.delete(file.toPath());
				}
				
				writeFileToZip(zipFile, entry, file);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			if (zipFile != null)
				zipFile.close();
		}
		
	}

	private static void writeFileToZip(ZipFile zipFile, ZipEntry entry, File file) throws IOException {
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		
		try {
			in = new BufferedInputStream(zipFile.getInputStream(entry));
			out = new BufferedOutputStream(new FileOutputStream(file));
			
			byte[] buf = new byte[2048];
			int size = -1;
			while ((size = in.read(buf)) != -1) {
				out.write(buf, 0, size);
			}
		} finally {
			NodeUtils.close(in);
			NodeUtils.close(out);
		}
	}

	private static File getFile(File targetFolder, String entryPath) {
		return new File(targetFolder, entryPath);
	}
	
	public static void addVmParametersForIgnite(List<String> cmdList) {
		String javaVersion = System.getProperty("java.version");
		
		if (javaVersion.startsWith("11.")) {
			addJava11VMParametersForIgnite(cmdList);
		} else {
			addJava17VMParametersForIgnite(cmdList);			
		}
	}

	private static void addJava17VMParametersForIgnite(List<String> cmdList) {
		throw new UnsupportedOperationException("Run in Java17 not supported yet!");
	}

	private static void addJava11VMParametersForIgnite(List<String> cmdList) {
		cmdList.add("--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED");
		cmdList.add("--add-exports=java.base/sun.nio.ch=ALL-UNNAMED");
		cmdList.add("--add-exports=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED");
		cmdList.add("--add-exports=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED");
		cmdList.add("--add-exports=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED");
		cmdList.add("--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED");
		cmdList.add("--illegal-access=permit");
	}
}
