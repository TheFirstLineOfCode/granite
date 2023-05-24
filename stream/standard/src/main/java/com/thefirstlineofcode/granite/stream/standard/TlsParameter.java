package com.thefirstlineofcode.granite.stream.standard;

public class TlsParameter {
	public static final Object KEY_TLS_PARAMETER = new Object();
	
	private String securityDir;
	private String hostName;
	
	public TlsParameter(String securityDir, String hostName) {
		this.securityDir = securityDir;
		this.hostName = hostName;
	}

	public String getSecurityDir() {
		return securityDir;
	}

	public String getHostName() {
		return hostName;
	}

}
