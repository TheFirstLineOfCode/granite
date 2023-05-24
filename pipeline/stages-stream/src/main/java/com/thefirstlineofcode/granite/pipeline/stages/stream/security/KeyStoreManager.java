package com.thefirstlineofcode.granite.pipeline.stages.stream.security;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.granite.framework.core.utils.IoUtils;

public class KeyStoreManager {
	private static final Logger logger = LoggerFactory.getLogger(KeyStoreManager.class);
	
	private static final String DEFAULT_KEY_MANAGEMENT_ALGORITHM = "SunX509";
	private File keyStoreFile;
	private char[] password;
	private KeyStore keyStore;
	private boolean created;
	
	static {
		SecurityUtils.registerJcaProviderIfNeed();
	}
	
	public KeyStoreManager(File keyStoreFile, String password) {
		this.keyStoreFile = keyStoreFile;
		this.password = password.toCharArray();
		created = false;
		
		try {
			loadOrCreateKeyStore();
		} catch (SecurityException e) {
			logger.error("Can't initialize 'KeyStoreManager'.", e);
			throw new RuntimeException("Can't initialize 'KeyStoreManager'.", e);
		}
	}
	
	private void loadOrCreateKeyStore() throws SecurityException {
		if (!keyStoreFile.exists())
			created = true;
		
		keyStore = SecurityUtils.loadOrCreateKeyStore(keyStoreFile, password);
	}
	
	public void save() throws SecurityException {
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(keyStoreFile));
			keyStore.store(out, password);
		} catch (Exception e) {
			logger.error("Can't save key store.", e);
			throw new SecurityException("Can't save key store.", e);
		} finally {
			IoUtils.closeIO(out);
		}
	}
	
	public boolean isCreated() {
		return created;
	}
	
	public boolean isLoad() {
		return !created;
	}
	
	public void addCertificate(String alias, Certificate certificate) throws SecurityException {
		try {
			keyStore.setCertificateEntry(alias, certificate);
		} catch (KeyStoreException e) {
			logger.error("Can't add a certificate to key store.", e);
			throw new SecurityException("Can't add a certificate to key store.", e);
		}
	}
	
	public Certificate getCertificate(String alias) throws SecurityException {
		try {
			return keyStore.getCertificate(alias);
		} catch (KeyStoreException e) {
			logger.error("Can't retrieve the certificate from key store.", e);
			throw new SecurityException("Can't retrieve the certificate from key store.", e);
		}
	}

	public void addKey(String alias, Key key, Certificate[] chain, String password) throws SecurityException {
		try {
			keyStore.setKeyEntry(alias, key, password.toCharArray(), chain);
		} catch (Exception e) {
			logger.error("Can't add a key to key store.", e);
			throw new SecurityException("Can't add a key to key store.", e);
		}
	}
	
	public Key getKey(String alias, String password) throws SecurityException {
		try {
			return keyStore.getKey(alias, password.toCharArray());
		} catch (Exception e) {
			logger.error("Can't retieve the key from key store.", e);
			throw new SecurityException("Can't retieve the key from key store.", e);
		}
	}
	
	public KeyManager[] getKeyManagers(String password) throws SecurityException {
		try {
			KeyManagerFactory factory = KeyManagerFactory.getInstance(DEFAULT_KEY_MANAGEMENT_ALGORITHM);
			factory.init(keyStore, password.toCharArray());
			
			return factory.getKeyManagers();
		} catch (Exception e) {
			logger.error("Can't retrieve key managers.", e);
			throw new SecurityException("Can't retrieve key managers.", e);
		}
	}
	
	public TrustManager[] getTrustManagers() throws SecurityException {
		try {
			TrustManagerFactory factory = TrustManagerFactory.getInstance(DEFAULT_KEY_MANAGEMENT_ALGORITHM);
			factory.init(keyStore);
			
			return factory.getTrustManagers();
		} catch (Exception e) {
			logger.error("Can't retrieve trust managers.", e);
			throw new SecurityException("Can't retrieve trust managers.", e);
		}
	}
}
