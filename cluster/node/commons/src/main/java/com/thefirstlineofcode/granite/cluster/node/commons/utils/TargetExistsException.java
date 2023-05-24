package com.thefirstlineofcode.granite.cluster.node.commons.utils;

import java.io.IOException;

public class TargetExistsException extends IOException {
	private static final long serialVersionUID = -751203845633813854L;

	public TargetExistsException() {
		super();
	}

	public TargetExistsException(String message, Throwable cause) {
		super(message, cause);
	}

	public TargetExistsException(String message) {
		super(message);
	}

	public TargetExistsException(Throwable cause) {
		super(cause);
	}
	
}
