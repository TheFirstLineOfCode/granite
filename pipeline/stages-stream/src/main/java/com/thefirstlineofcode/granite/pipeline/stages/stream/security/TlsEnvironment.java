package com.thefirstlineofcode.granite.pipeline.stages.stream.security;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.cert.Certificate;
import java.util.Calendar;
import java.util.Date;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TlsEnvironment {
	private static final String SIGNATURE_ALGORITHM_SHA256_WITH_RSA = "SHA256withRSA";
	private static final String ALGORITHM_RSA = "RSA";
	private static final String ALIAS_HOST_KEY = "host-key";
	private static final String NAME_KEY_STORE = "keystore";
	private static final String DEFAULT_PASSWORD = "changeit";
	private static final String PROTOCOL_TLS = "TLSv1.2";
	private File securityDirectory;
	private String hostName;
	
	private static final Logger logger = LoggerFactory.getLogger(TlsEnvironment.class);
	
	public TlsEnvironment(String sSecurityDirectory, String hostName) {
		securityDirectory = new File(sSecurityDirectory);
		this.hostName = hostName;
	}
	
	public SSLContext getSslContext() throws SecurityException {
		if (!ready()) {
			logger.error("TLS environment isn't ready.");
			throw new SecurityException("TLS environment isn't ready.");
		}
		
		KeyStoreManager keyStoreManager = new KeyStoreManager(new File(
				securityDirectory, NAME_KEY_STORE), DEFAULT_PASSWORD);
		try {
			SSLContext sslContext = SSLContext.getInstance(PROTOCOL_TLS);
			sslContext.init(keyStoreManager.getKeyManagers(DEFAULT_PASSWORD), null, null);
						
			return sslContext;
		} catch (Exception e) {
			logger.error("Can't get SSL context.", e);
			throw new SecurityException("Can't get SSL context.", e);
		}
	}

	public boolean ready() {
		if (!securityDirectory.exists() ||
				!securityDirectory.isDirectory())
			return false;
		
		return new File(securityDirectory, NAME_KEY_STORE).exists();
	}

	public void init() throws SecurityException, IOException {
		if (!securityDirectory.exists()) {
			logger.info("Security directory doesn't exist. Creating it...");
			
			if (!securityDirectory.mkdirs()) {
				logger.error("Can't create security directory.");
				throw new SecurityException("Can't create security directory.");
			} else {
				logger.info("Security directory created.");
			}
		}
		
		File keyStore = new File(securityDirectory, NAME_KEY_STORE);
		if (keyStore.exists())
			return;
		
		logger.info("Key store doesn't exist. Creating it...");
		
		KeyStoreManager keyStoreManager = new KeyStoreManager(keyStore, DEFAULT_PASSWORD);
		KeyPair keyPair = SecurityUtils.createKeyPair(ALGORITHM_RSA, 1024);
		
		Calendar now = Calendar.getInstance();
		Date startDate = now.getTime();

		// enough time to expire(100 years)
		Calendar hundredYearsLater = Calendar.getInstance();
		hundredYearsLater.set(Calendar.YEAR, now.get(Calendar.YEAR) + 100);
		Date expireDate = hundredYearsLater.getTime();
		
		Certificate certificate = SecurityUtils.createX509Certificate(hostName, startDate, expireDate, keyPair, SIGNATURE_ALGORITHM_SHA256_WITH_RSA);
		keyStoreManager.addKey(ALIAS_HOST_KEY, keyPair.getPrivate(), new Certificate[] {certificate}, DEFAULT_PASSWORD);
		
		keyStoreManager.save();
		
		logger.info("Key store created.");
	}
}
