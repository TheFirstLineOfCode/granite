package com.thefirstlineofcode.granite.framework.core.pipeline.stages;

import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventListenerFactory;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.parsing.IPipelinePreprocessor;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.parsing.IProtocolParserFactory;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.parsing.CocProtocolParserFactory;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IIqResultProcessor;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IXepProcessorFactory;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.IPipelinePostprocessor;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.IProtocolTranslatorFactory;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.CocProtocolTranslatorFactory;
import com.thefirstlineofcode.granite.framework.core.session.ISessionListener;

public class PipelineExtendersContributorAdapter implements IPipelineExtendersContributor {

	@Override
	public IProtocolParserFactory<?>[] getProtocolParserFactories() {		
		IProtocolParserFactory<?>[] namingConventionParserFactories = getCocParserFactories();
		IProtocolParserFactory<?>[] customizedParserFactories = getCustomizedParserFactories();
		
		return merge(namingConventionParserFactories, customizedParserFactories);
	}

	protected IProtocolParserFactory<?>[] merge(IProtocolParserFactory<?>[] namingConventionParserFactories,
			IProtocolParserFactory<?>[] customizedParserFactories) {
		if (isEmpty(namingConventionParserFactories) && isEmpty(customizedParserFactories)) {
			return null;
		}
		
		if (isEmpty(customizedParserFactories))
			return namingConventionParserFactories;
		
		if (isEmpty(namingConventionParserFactories))
			return customizedParserFactories;
		
		int length = namingConventionParserFactories.length + customizedParserFactories.length;
		IProtocolParserFactory<?>[] allParserFactories = new IProtocolParserFactory<?>[length];
		
		for (int i = 0; i < namingConventionParserFactories.length; i++) {
			allParserFactories[i] = namingConventionParserFactories[i];
		}
		
		for (int i = 0; i < customizedParserFactories.length; i++) {
			allParserFactories[namingConventionParserFactories.length + i] = customizedParserFactories[i];
		}
		
		return allParserFactories;
	}
	
	protected IProtocolTranslatorFactory<?>[] merge(IProtocolTranslatorFactory<?>[] namingConventionTranslatorFactories,
			IProtocolTranslatorFactory<?>[] customizedTranslatorFactories) {
		if (isEmpty(namingConventionTranslatorFactories) && isEmpty(customizedTranslatorFactories)) {
			return null;
		}
		
		if (isEmpty(customizedTranslatorFactories))
			return namingConventionTranslatorFactories;
		
		if (isEmpty(namingConventionTranslatorFactories))
			return customizedTranslatorFactories;
		
		int length = namingConventionTranslatorFactories.length + customizedTranslatorFactories.length;
		IProtocolTranslatorFactory<?>[] allParserFactories = new IProtocolTranslatorFactory<?>[length];
		
		for (int i = 0; i < namingConventionTranslatorFactories.length; i++) {
			allParserFactories[i] = namingConventionTranslatorFactories[i];
		}
		
		for (int i = 0; i < customizedTranslatorFactories.length; i++) {
			allParserFactories[namingConventionTranslatorFactories.length + i] = customizedTranslatorFactories[i];
		}
		
		return allParserFactories;
	}

	private <T> boolean isEmpty(T[] factories) {
		return factories == null || factories.length == 0;
	}

	protected IProtocolParserFactory<?>[] getCocParserFactories() {
		CocParsableProtocolObject[] cocParsableProtocolObjects =
				getCocParsableProtocolObjects();
		if (cocParsableProtocolObjects == null || cocParsableProtocolObjects.length == 0)
			return null;
		
		IProtocolParserFactory<?>[] cocParserFactories = new IProtocolParserFactory<?>[cocParsableProtocolObjects.length];
		for (int i = 0; i < cocParsableProtocolObjects.length; i++) {
			cocParserFactories[i] = new CocProtocolParserFactory<>(
					cocParsableProtocolObjects[i].protocolChain, cocParsableProtocolObjects[i].protocolObjectClass);
		}
		
		return cocParserFactories;
	}
	
	protected IProtocolParserFactory<?>[] getCustomizedParserFactories() {
		return null;
	}

	protected CocParsableProtocolObject[] getCocParsableProtocolObjects() {
		return null;
	}

	@Override
	public IXepProcessorFactory<?, ?>[] getXepProcessorFactories() {
		return null;
	}

	@Override
	public IProtocolTranslatorFactory<?>[] getProtocolTranslatorFactories() {
		IProtocolTranslatorFactory<?>[] namingConventionTranslatorFactories = getNamingConventionTranslatorFactories();
		IProtocolTranslatorFactory<?>[] customizedTranslatorFactories = getCustomizedTranslatorFactories();
		
		return merge(namingConventionTranslatorFactories, customizedTranslatorFactories);
	}

	protected IProtocolTranslatorFactory<?>[] getCustomizedTranslatorFactories() {
		return null;
	}

	private IProtocolTranslatorFactory<?>[] getNamingConventionTranslatorFactories() {
		Class<?>[] cocTranslatableProtocolObjects = getCocTranslatableProtocolObjects();
		if (cocTranslatableProtocolObjects == null || cocTranslatableProtocolObjects.length == 0)
			return null;
		
		IProtocolTranslatorFactory<?>[] cocTranslatorFactories = new IProtocolTranslatorFactory<?>[cocTranslatableProtocolObjects.length];
		for (int i = 0; i < cocTranslatableProtocolObjects.length; i++) {
			cocTranslatorFactories[i] = new CocProtocolTranslatorFactory<>(cocTranslatableProtocolObjects[i]);
		}
		
		return cocTranslatorFactories;
	}
	
	protected Class<?>[] getCocTranslatableProtocolObjects() {
		return null;
	}

	@Override
	public IPipelinePreprocessor[] getPipelinePreprocessors() {
		return null;
	}

	@Override
	public IPipelinePostprocessor[] getPipelinePostprocessors() {
		return null;
	}

	@Override
	public IEventListenerFactory<?>[] getEventListenerFactories() {
		return null;
	}

	@Override
	public IIqResultProcessor[] getIqResultProcessors() {
		return null;
	}
	
	@Override
	public ISessionListener[] getSessionListeners() {
		return null;
	}
	
	protected class CocParsableProtocolObject {
		public ProtocolChain protocolChain;
		public Class<?> protocolObjectClass;
		
		public CocParsableProtocolObject(ProtocolChain protocolChain, Class<?> protocolObjectClass) {
			this.protocolChain = protocolChain;
			this.protocolObjectClass = protocolObjectClass;
		}
	}
}
