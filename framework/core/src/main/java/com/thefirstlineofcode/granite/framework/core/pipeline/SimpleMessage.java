package com.thefirstlineofcode.granite.framework.core.pipeline;

import java.util.Collections;
import java.util.Map;

public class SimpleMessage implements IMessage {
	protected Map<Object, Object> headers;
	protected Object payload;
	
	public SimpleMessage() {
		this(null, null);
	}
	
	public SimpleMessage(Object payload) {
		this(null, payload);
	}
	
	public SimpleMessage(Map<Object, Object> headers, Object payload) {
		this.headers = headers;
		this.payload = payload;
	}
	
	public void setHeaders(Map<Object, Object> headers) {
		this.headers = headers;
	}

	@Override
	public Map<Object, Object> getHeaders() {
		if (headers == null) {
			headers = Collections.emptyMap();
		}
		
		return Collections.unmodifiableMap(headers);
	}
	
	public void setPayload(Object payload) {
		this.payload = payload;
	}

	@Override
	public Object getPayload() {
		return payload;
	}

}
