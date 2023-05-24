package com.thefirstlineofcode.granite.framework.core.pipeline.stages.parsing;

import com.thefirstlineofcode.basalt.oxm.parsing.IParser;
import com.thefirstlineofcode.basalt.oxm.parsing.IParserFactory;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;

public class ProtocolParserFactory<T> implements IProtocolParserFactory<T> {
	private ProtocolChain protocolChain;
	private IParserFactory<T> parserFactory;
	
	public ProtocolParserFactory(ProtocolChain protocolChain, IParserFactory<T> parserFactory) {
		this.protocolChain = protocolChain;
		this.parserFactory = parserFactory;
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
