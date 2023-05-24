package com.thefirstlineofcode.granite.framework.core.adf;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Enumeration;

import com.thefirstlineofcode.granite.framework.core.utils.CompositeEnumeration;

public class CompositeClassLoader extends ClassLoader {
	private ClassLoader[] classLoaders;
	
	public CompositeClassLoader(ClassLoader[] classLoaders) {
		if (classLoaders == null || classLoaders.length == 0)
			throw new IllegalArgumentException("Null class loaders or no any class loader.");
		
		this.classLoaders = classLoaders;
	}
	
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		for (ClassLoader classLoader : classLoaders) {
			try {
				Class<?> clazz = classLoader.loadClass(name);
				if (clazz != null)
					return clazz;
			} catch (ClassNotFoundException e) {
				// Ignore
			}
		}
		
		throw new ClassNotFoundException(name);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		Enumeration<URL>[] allResources = (Enumeration<URL>[])Array.newInstance(Enumeration.class, classLoaders.length);
		for (int i = 0; i < classLoaders.length; i++) {
			allResources[i] = classLoaders[i].getResources(name);
		}
		
		return new CompositeEnumeration<URL>(allResources);
	}
	
	@Override
	public URL getResource(String name) {
		for (ClassLoader classLoader : classLoaders) {
			URL url = classLoader.getResource(name);
			if (url != null)
				return url;
		}
		
		return null;
	}
}
