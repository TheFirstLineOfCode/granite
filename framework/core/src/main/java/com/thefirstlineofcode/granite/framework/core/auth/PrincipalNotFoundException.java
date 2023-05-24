package com.thefirstlineofcode.granite.framework.core.auth;

public class PrincipalNotFoundException extends Exception {
	private static final long serialVersionUID = 3672342152289363647L;

	public PrincipalNotFoundException() {
		super();
	}

	public PrincipalNotFoundException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public PrincipalNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public PrincipalNotFoundException(String message) {
		super(message);
	}

	public PrincipalNotFoundException(Throwable cause) {
		super(cause);
	}
	
}
