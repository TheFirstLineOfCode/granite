package com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing;

import com.thefirstlineofcode.basalt.oxm.translating.ITranslator;
import com.thefirstlineofcode.basalt.oxm.translating.ITranslatorFactory;
import com.thefirstlineofcode.basalt.oxm.translators.SimpleObjectTranslatorFactory;
import com.thefirstlineofcode.basalt.xmpp.core.Protocol;

public class SimpleObjectProtocolTranslatorFactory<T> implements IProtocolTranslatorFactory<T> {
	private ITranslatorFactory<T> translatorFactory;
	
	public SimpleObjectProtocolTranslatorFactory(Class<T> type, Protocol protocol) {
		translatorFactory = new SimpleObjectTranslatorFactory<>(type, protocol);
	}

	@Override
	public Class<T> getType() {
		return translatorFactory.getType();
	}

	@Override
	public ITranslator<T> createTranslator() {
		return translatorFactory.create();
	}

}
