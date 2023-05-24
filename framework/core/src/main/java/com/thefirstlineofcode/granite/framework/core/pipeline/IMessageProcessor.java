package com.thefirstlineofcode.granite.framework.core.pipeline;

import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;

public interface IMessageProcessor {
	void process(IConnectionContext context, IMessage message);
}
