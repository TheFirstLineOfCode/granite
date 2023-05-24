package com.thefirstlineofcode.granite.pipeline.stages.stream.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.granite.framework.core.utils.IoUtils;

public class SecurityUtils {
	private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);
	
	public static final Provider DEFAULT_JCA_PROVIDER = new BouncyCastleProvider();
	public static final String DEFAULT_JCA_PROVIDER_NAME = BouncyCastleProvider.PROVIDER_NAME;
	public static final String DEFAULT_KEY_STORE_TYPE = "PKCS12";
	private static final String SECURE_RANDOM_ALGORITHM = "SHA1PRNG";
	
	public static void registerJcaProviderIfNeed() {
		registerJcaProviderIfNeed(DEFAULT_JCA_PROVIDER_NAME, DEFAULT_JCA_PROVIDER);
	}
	
	public static void registerJcaProviderIfNeed(String providerName, Provider provider) {
		if (Security.getProvider(providerName) == null)
			Security.addProvider(provider);
	}
	
	public static KeyPair createKeyPair(String algorithm, int keySize) throws SecurityException {
		return createKeyPair(algorithm, keySize, DEFAULT_JCA_PROVIDER_NAME);
	}
	
	public static KeyPair createKeyPair(String algorithm, int keySize, String provider) throws SecurityException {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm, provider);
			keyPairGenerator.initialize(keySize);
			
			return keyPairGenerator.genKeyPair();
		} catch (Exception e) {
			logger.error("Failed to create key pair.", e);
			throw new SecurityException("Failed to create key pair.", e);
		}
	}
	
	public static X509Certificate createX509Certificate(String hostName, Date startDate, Date expireDate,
				KeyPair keyPair, String signatureAlgorithm) throws SecurityException {
		try {
	        byte[] serno = new byte[8];
	        SecureRandom random = SecureRandom.getInstance(SECURE_RANDOM_ALGORITHM);
	        random.setSeed((new Date().getTime()));
	        random.nextBytes(serno);
	        BigInteger serialNumber = (new java.math.BigInteger(serno)).abs();
	        
	        return createX509Certificate(serialNumber, hostName, startDate,
	        		expireDate, keyPair, signatureAlgorithm);
		} catch (Exception e) {
			if (e instanceof SecurityException)
				throw (SecurityException)e;
			
			logger.error("Can't create a certificate.", e);
			throw new SecurityException("Can't create a certificate.", e);
		}

	}
	
	public static X509Certificate createX509Certificate(BigInteger serialNumber, String hostName,
			Date startDate, Date expireDate, KeyPair keyPair, String signatureAlgorithm) throws SecurityException {
		return createX509Certificate(serialNumber, hostName, startDate, expireDate, keyPair,
				signatureAlgorithm, null, null);
	}

	public static X509Certificate createX509Certificate(BigInteger serialNumber, String hostName,
			Date startDate, Date expireDate, KeyPair keyPair, String signatureAlgorithm,
			X509Certificate parentCertificate, PrivateKey parentPrivateKey) throws SecurityException {
		boolean caSignature = false;
		if (parentCertificate != null && parentPrivateKey != null)
			caSignature = true;
		
		String subjectDNX500Name = "CN=" + hostName;
		
		X500Name issuerDNX500Name;
		if (caSignature) {
			issuerDNX500Name = new X500Name(parentCertificate.getSubjectX500Principal().getName());
		} else {
			issuerDNX500Name = new X500Name(subjectDNX500Name);
		}
		X509v3CertificateBuilder builder = new X509v3CertificateBuilder(issuerDNX500Name, serialNumber, startDate,
				expireDate, new X500Name(subjectDNX500Name), SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));
		
		try {
			if (caSignature) {
				builder.addExtension(X509Extension.authorityKeyIdentifier,
						false, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(parentCertificate));
			} else {
				builder.addExtension(X509Extension.authorityKeyIdentifier,
						false, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())));
			}
			
			if (isIpv4Address(hostName)) {
				InetAddress inetAddress = InetAddress.getByName(hostName);
				GeneralNames subjectAlterniativeNames = new GeneralNames(new GeneralName(GeneralName.iPAddress, new DEROctetString(inetAddress.getAddress())));
				builder.addExtension(X509Extension.subjectAlternativeName, false, subjectAlterniativeNames);
			}
			
			builder.addExtension(X509Extension.subjectKeyIdentifier,
					false, new JcaX509ExtensionUtils().createSubjectKeyIdentifier(SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())));
			builder.addExtension(X509Extension.basicConstraints, false, new BasicConstraints(false));
			builder.addExtension(X509Extension.keyUsage, false, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
			builder.addExtension(X509Extension.extendedKeyUsage, false, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
			
			ContentSigner signer;
			if (caSignature) {
				 signer = new JcaContentSignerBuilder(signatureAlgorithm).setProvider(DEFAULT_JCA_PROVIDER_NAME).build(parentPrivateKey);
			} else {
				signer = new JcaContentSignerBuilder(signatureAlgorithm).setProvider(DEFAULT_JCA_PROVIDER_NAME).build(keyPair.getPrivate());
			}
			
			return new JcaX509CertificateConverter().setProvider(DEFAULT_JCA_PROVIDER_NAME).getCertificate(builder.build(signer));
		} catch (Exception e) {
			logger.error("Can't create a certificate.", e);
			throw new SecurityException("Can't create a certificate.", e);
		}
	}
	
	private static boolean isIpv4Address(String address) {
		String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";    
		
		return address.matches(PATTERN);
	}
	
	public static KeyStore loadOrCreateKeyStore(File keyStoreFile, char[] password) throws SecurityException {
		return loadOrCreateKeyStore(keyStoreFile, password, DEFAULT_KEY_STORE_TYPE, DEFAULT_JCA_PROVIDER_NAME);
	}
	
	public static KeyStore loadOrCreateKeyStore(File keyStoreFile, char[] password,
				String keyStoreType, String provider) throws SecurityException {
		InputStream in = null;
		OutputStream out = null;
		
		try {
			if (keyStoreFile.exists())
				in = new FileInputStream(keyStoreFile);
			
			KeyStore keyStore = KeyStore.getInstance(keyStoreType, provider);
			keyStore.load(in, password);
			
			if (!keyStoreFile.exists()) {
				out = new FileOutputStream(keyStoreFile);
				keyStore.store(out, password);
			}
			
			return keyStore;
		} catch (Exception e) {
			logger.error(String.format("Failed to create key store '%s'.", keyStoreFile), e);
			throw new SecurityException(String.format("Failed to create key store '%s'.", keyStoreFile), e);
		} finally {
			IoUtils.closeIO(in);
			IoUtils.closeIO(out);
		}
	}
}
