package com.thefirstlineofcode.granite.framework.core.session;

public class SessionExistsException extends Exception {

	private static final long serialVersionUID = -9106286812874246689L;

	public SessionExistsException() {
		super();
	}

	public SessionExistsException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public SessionExistsException(String message, Throwable cause) {
		super(message, cause);
	}

	public SessionExistsException(String message) {
		super(message);
	}

	public SessionExistsException(Throwable cause) {
		super(cause);
	}
	
}
