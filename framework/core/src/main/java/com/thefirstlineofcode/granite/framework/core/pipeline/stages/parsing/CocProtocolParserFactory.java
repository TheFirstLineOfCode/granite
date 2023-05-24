package com.thefirstlineofcode.granite.framework.core.pipeline.stages.parsing;

import com.thefirstlineofcode.basalt.oxm.coc.CocParserFactory;
import com.thefirstlineofcode.basalt.oxm.parsing.IParser;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;

public class CocProtocolParserFactory<T> implements IProtocolParserFactory<T> {
	private ProtocolChain protocolChain;
	private CocParserFactory<T> parserFactory;
	
	public CocProtocolParserFactory(ProtocolChain protocolChain, Class<T> type) {
		this.protocolChain = protocolChain;
		parserFactory = new CocParserFactory<T>(type);
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
