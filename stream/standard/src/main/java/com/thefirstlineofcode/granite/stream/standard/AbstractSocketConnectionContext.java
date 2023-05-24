package com.thefirstlineofcode.granite.stream.standard;

import java.net.InetSocketAddress;

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.session.IoSession;

import com.thefirstlineofcode.granite.framework.core.connection.IClientConnectionContext;

public abstract class AbstractSocketConnectionContext implements IClientConnectionContext {
	protected IoSession session;
	protected String localNodeId;

	public AbstractSocketConnectionContext(IoSession session, String localNodeId) {
		this.session = session;
		this.localNodeId = localNodeId;
	}

	@Override
	public void write(Object message) {
		session.write(message);
	}

	@Override
	public void close() {
		close(false);
	}
	
	@Override
	public boolean close(boolean sync) {
	    CloseFuture future = session.closeOnFlush();
	    if (sync) {
	        future.awaitUninterruptibly();
	    }
	
	    return future.isClosed();
	
	}
	
	@Override
	public boolean isTlsSupported() {
		return false;
	}

	@Override
	public boolean isTlsStarted() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void startTls() {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <K> K setAttribute(Object key, K value) {
		return (K)session.setAttribute(key, value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <K> K getAttribute(Object key) {
		return (K)session.getAttribute(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <K> K getAttribute(Object key, K defaultValue) {
		return (K)session.getAttribute(key, defaultValue);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <K> K removeAttribute(Object key) {
		return (K)session.removeAttribute(key);
	}

	@Override
	public Object[] getAttributeKeys() {
		return session.getAttributeKeys().toArray();
	}

	@Override
	public Object getConnectionId() {
		return session.getId();
	}

	@Override
	public String getRemoteIp() {
		return ((InetSocketAddress)session.getRemoteAddress()).getAddress().getHostAddress();
	}

	@Override
	public int getRemotePort() {
		return ((InetSocketAddress)session.getRemoteAddress()).getPort();
	}

}