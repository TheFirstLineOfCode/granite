package com.thefirstlineofcode.granite.framework.core.pipeline;

import java.util.Map;

import com.thefirstlineofcode.granite.framework.core.session.ISession;

public interface IMessage {
	public static final String KEY_SESSION_JID = ISession.KEY_SESSION_JID;
	public static final String KEY_MESSAGE_TARGET = "granite.message.target";
	
	Map<Object, Object> getHeaders();
	Object getPayload();
}
