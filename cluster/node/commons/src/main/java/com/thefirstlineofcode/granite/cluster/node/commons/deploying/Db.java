package com.thefirstlineofcode.granite.cluster.node.commons.deploying;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Db {
	private List<DbAddress> addresses;
	private String userName;
	private byte[] password;
	private String dbName;
	
	public Db() {
		this((String)null);
	}
	
	public Db(String host) {
		this(host, 27017);
	}
	
	public Db(String host, int port) {
		this(new DbAddress(host, port));
	}
	
	public Db(DbAddress address) {
		this(Collections.singletonList(address));
	}
	
	public Db(List<DbAddress> addresses) {
		this.addresses = addresses;
	}
	
	@Override
	public String toString() {
		if (addresses == null || addresses.isEmpty()) {
			return null;
		}
		
		Collections.sort(addresses, new Comparator<DbAddress>() {
			@Override
			public int compare(DbAddress address1, DbAddress address2) {
				int hostComparedResult = address1.getHost().compareTo(address2.getHost());
				if (hostComparedResult != 0)
					return hostComparedResult;
				
				return address1.getPort() - address2.getPort();
			}
		});
		
		StringBuilder sb = new StringBuilder();
		for (DbAddress address : addresses) {
			sb.append(address.getHost()).append(':').append(address.getPort()).append(",");
		}
		
		if (sb.length() > 0) {
			sb.delete(sb.length() - 1, sb.length());
		}
		
		return userName + "@" + sb.toString() + "/" + dbName;
	}
	
	public List<DbAddress> getAddresses() {
		return addresses;
	}

	public void setAddresses(List<DbAddress> addresses) {
		this.addresses = addresses;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public byte[] getPassword() {
		return password;
	}

	public void setPassword(byte[] password) {
		this.password = password;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}
	
}
