package com.thefirstlineofcode.granite.cluster.node.commons.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IoUtils {
	private static final Logger logger = LoggerFactory.getLogger(IoUtils.class);

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
			IoUtils.close(out);
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
			IoUtils.close(BufferedIn);
			IoUtils.close(out);
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
			IoUtils.close(in);
		}
	}
}
