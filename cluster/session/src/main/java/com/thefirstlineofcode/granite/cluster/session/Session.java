package com.thefirstlineofcode.granite.cluster.session;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.session.ISession;
import com.thefirstlineofcode.granite.framework.core.session.ValueWrapper;

public class Session implements ISession {
	private JabberId id;
	private ConcurrentMap<Object, Object> attributes;
	
	public Session(JabberId id) {
		this.id = id;
		attributes = new ConcurrentHashMap<>();
	}

	@Override
	public <T> T setAttribute(Object key, T value) {
		Object previous = attributes.putIfAbsent(key, value);
		if (previous != null)
			return unwrapIfNeed(previous);
		
		return value;
	}

	@Override
	public <T> T getAttribute(Object key) {
		return unwrapIfNeed(attributes.get(key));
	}

	@Override
	public <T> T getAttribute(Object key, T defaultValue) {
		T value = getAttribute(key);
		
		return value != null ? value : defaultValue;
	}

	@Override
	public <T> T removeAttribute(Object key) {
		return unwrapIfNeed(attributes.remove(key));
	}

	@SuppressWarnings("unchecked")
	private <T> T unwrapIfNeed(Object value) {
		if (value == null)
			return null;
		
		if (value instanceof ValueWrapper) {
			ValueWrapper<T> valueWrapper = (ValueWrapper<T>)value;
			return (T)valueWrapper.getValue();
		} else {
			return (T)value;
		}
	}

	@Override
	public Object[] getAttributeKeys() {
		return attributes.keySet().toArray();
	}

	@Override
	public JabberId getJid() {
		return id;
	}
	
	@Override
	public <T> T setAttribute(Object key, ValueWrapper<T> wrapper) {
		Object previous = attributes.putIfAbsent(key, wrapper);
		if (previous != null) {
			return unwrapIfNeed(previous);
		}
		
		return wrapper.getValue();
	}

}
