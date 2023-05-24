package com.thefirstlineofcode.granite.cluster.node.commons.deploying;

public class DeployPlanException extends Exception {

	private static final long serialVersionUID = 4043610397350844765L;

	public DeployPlanException() {
		super();
	}

	public DeployPlanException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DeployPlanException(String message, Throwable cause) {
		super(message, cause);
	}

	public DeployPlanException(String message) {
		super(message);
	}

	public DeployPlanException(Throwable cause) {
		super(cause);
	}
	
}
