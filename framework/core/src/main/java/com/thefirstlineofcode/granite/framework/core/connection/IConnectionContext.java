package com.thefirstlineofcode.granite.framework.core.connection;

import com.thefirstlineofcode.granite.framework.core.session.ISession;

public interface IConnectionContext extends ISession {
	void write(Object message);
	void close();
}
