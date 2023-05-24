package com.thefirstlineofcode.granite.framework.core.connection;


public interface IClientConnectionContext extends IConnectionContext {
	boolean close(boolean sync);
	
	Object getConnectionId();
	String getRemoteIp();
	int getRemotePort();
	
	boolean isTlsSupported();
	boolean isTlsStarted();
	void startTls();
	
	String getLocalNodeId();
	String getStreamId();
}
