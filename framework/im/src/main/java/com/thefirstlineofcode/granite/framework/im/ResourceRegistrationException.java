package com.thefirstlineofcode.granite.framework.im;

public class ResourceRegistrationException extends Exception {

	private static final long serialVersionUID = 2766524035506782605L;

	public ResourceRegistrationException() {
		super();
	}

	public ResourceRegistrationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ResourceRegistrationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResourceRegistrationException(String message) {
		super(message);
	}

	public ResourceRegistrationException(Throwable cause) {
		super(cause);
	}
	
}
