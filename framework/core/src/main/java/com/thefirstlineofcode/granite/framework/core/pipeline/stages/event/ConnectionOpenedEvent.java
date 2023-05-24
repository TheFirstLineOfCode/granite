package com.thefirstlineofcode.granite.framework.core.pipeline.stages.event;

public class ConnectionOpenedEvent implements IEvent {
	private String connectionId;
	private String ip;
	private int port;
	
	public ConnectionOpenedEvent(String connectionId, String ip, int port) {
		this.connectionId = connectionId;
		this.ip = ip;
		this.port = port;
	}
	
	public String getIp() {
		return ip;
	}
	
	public int getPort() {
		return port;
	}
	
	public String getConnectionId() {
		return connectionId;
	}
	
	@Override
	public Object clone() {
		return new ConnectionOpenedEvent(connectionId, ip, port);
	}
	
	@Override
	public String toString() {
		return String.format("ConnectionOpenedEvent[Connection ID=%s, IP=%s, Port=%d]",
				connectionId, ip, port);
	}
}
