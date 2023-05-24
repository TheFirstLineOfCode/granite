package com.thefirstlineofcode.granite.xeps.ibr;

public class MalformedRegistrationInfoException extends Exception {

	private static final long serialVersionUID = 9052405087147244556L;

	public MalformedRegistrationInfoException() {
		super();
	}

	public MalformedRegistrationInfoException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public MalformedRegistrationInfoException(String message, Throwable cause) {
		super(message, cause);
	}

	public MalformedRegistrationInfoException(String message) {
		super(message);
	}

	public MalformedRegistrationInfoException(Throwable cause) {
		super(cause);
	}
	
}
