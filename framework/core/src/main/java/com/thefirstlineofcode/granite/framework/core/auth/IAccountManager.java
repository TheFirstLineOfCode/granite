package com.thefirstlineofcode.granite.framework.core.auth;

public interface IAccountManager {
	void add(String userName, String password);
	void add(Account account);
	void remove(String name);
	boolean exists(String name);
	Account get(String name);
}
