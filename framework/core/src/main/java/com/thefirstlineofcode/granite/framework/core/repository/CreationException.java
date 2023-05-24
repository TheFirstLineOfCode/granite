package com.thefirstlineofcode.granite.framework.core.repository;

public class CreationException extends Exception {
	private static final long serialVersionUID = 6742802105775154879L;

	public CreationException() {
		super();
	}

	public CreationException(String message, Throwable cause) {
		super(message, cause);
	}

	public CreationException(String message) {
		super(message);
	}

	public CreationException(Throwable cause) {
		super(cause);
	}	
	
}
