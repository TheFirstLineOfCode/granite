package com.thefirstlineofcode.granite.framework.core.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IoUtils {
    private static final Logger logger = LoggerFactory.getLogger(IoUtils.class);
    
    public static void closeIO(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (Exception e) {
                logger.trace("Failed to close the stream.", e);
            }
        }
    }

    public static void closeIO(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (Exception e) {
                logger.trace("Failed to close the stream.", e);
            }
        }
    }

    public static void deleteFileRecursively(File file) throws IOException {
    	if (!file.exists())
    		return;
    	
        if (!file.isDirectory()) {
            Files.delete(file.toPath());
        } else {
            for (File subFile : file.listFiles()) {
                deleteFileRecursively(subFile);
            }

            Files.delete(file.toPath());
        }
    }

    public static void closeIO(Writer writer) {
        if (writer != null) {
            try {
                writer.close();
            } catch (Exception e) {
                logger.trace("Failed to close the writer.", e);
            }
        }
    }

    public static void closeIO(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (Exception e) {
                logger.trace("Failed to close the reader.", e);
            }
        }
    }

    public static void writeToFile(String content, File file) throws IOException {
    	FileWriter out = null;
        try {
        	out = new FileWriter(file);
            out.write(content);
        } catch (IOException e) {
        	throw e;
        } finally {
        	IoUtils.closeIO(out);
        }
    }
}
