package com.thefirstlineofcode.granite.cluster.pipeline;

import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageChannel;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageConnector;

@Component("cluster.stream.2.parsing.message.channel")
public class Stream2ParsingMessageChannel implements IMessageChannel {
	@Dependency("connector")
	private IMessageConnector connector;

	@Override
	public void send(IMessage message) {
		connector.put(message);
	}
}
