package com.thefirstlineofcode.granite.cluster.node.commons.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
	private static final String FILE_SEPARATOR = System.getProperty("file.separator");
	
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
			IoUtils.close(zos);
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
				
				writeFile(zipFile, entry, file);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			if (zipFile != null)
				zipFile.close();
		}
		
	}

	private static void writeFile(ZipFile zipFile, ZipEntry entry, File file) throws IOException {
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
			IoUtils.close(in);
			IoUtils.close(out);
		}
	}

	private static File getFile(File targetFolder, String entryPath) {
		return new File(targetFolder, entryPath);
	}
}
