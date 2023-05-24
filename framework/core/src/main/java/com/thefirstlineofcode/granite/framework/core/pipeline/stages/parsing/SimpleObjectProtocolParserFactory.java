package com.thefirstlineofcode.granite.framework.core.pipeline.stages.parsing;

import com.thefirstlineofcode.basalt.oxm.parsers.SimpleObjectParserFactory;
import com.thefirstlineofcode.basalt.oxm.parsing.IParser;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;

public class SimpleObjectProtocolParserFactory<T> implements IProtocolParserFactory<T> {
	private ProtocolChain protocolChain;
	private SimpleObjectParserFactory<T> parserFactory;
	
	public SimpleObjectProtocolParserFactory(ProtocolChain protocolChain, Class<T> type) {
		this.protocolChain = protocolChain;
		parserFactory = new SimpleObjectParserFactory<>(protocolChain.get(protocolChain.size() - 1), type);
	}

	@Override
	public ProtocolChain getProtocolChain() {
		return protocolChain;
	}

	@Override
	public IParser<T> createParser() {
		return parserFactory.create();
	}
}
