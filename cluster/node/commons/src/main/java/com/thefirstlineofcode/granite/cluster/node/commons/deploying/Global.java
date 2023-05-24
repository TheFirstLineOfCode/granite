package com.thefirstlineofcode.granite.cluster.node.commons.deploying;

public class Global {
	public static final String MESSAGE_FORMAT_XML = "xml";
	public static final String MESSAGE_FORMAT_BINARY = "binary";
	
	private int sessionDurationTime = 5 * 60;
	private String messageFormat = MESSAGE_FORMAT_XML;

	public int getSessionDurationTime() {
		return sessionDurationTime;
	}

	public void setSessionDurationTime(int sessionDurationTime) {
		this.sessionDurationTime = sessionDurationTime;
	}

	public String getMessageFormat() {
		return messageFormat;
	}

	public void setMessageFormat(String messageFormat) {
		this.messageFormat = messageFormat;
	}
	
	@Override
	public String toString() {
		return sessionDurationTime + "|" + messageFormat;
	}
	
}
