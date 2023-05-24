package com.thefirstlineofcode.granite.framework.core.pipeline.stages;

import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEvent;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventListener;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventListenerFactory;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.parsing.IPipelinePreprocessor;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.parsing.IProtocolParserFactory;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IIqResultProcessor;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IXepProcessor;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IXepProcessorFactory;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.IPipelinePostprocessor;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.IProtocolTranslatorFactory;
import com.thefirstlineofcode.granite.framework.core.session.ISessionListener;

public interface IPipelineExtendersConfigurator {
	IPipelineExtendersConfigurator registerCocParser(ProtocolChain protocolChain, Class<?> protocolObjectType);
	IPipelineExtendersConfigurator registerParserFactory(IProtocolParserFactory<?> parserFactory);
	
	IPipelineExtendersConfigurator registerSingletonXepProcessor(ProtocolChain protocolChain, IXepProcessor<?, ?> xepProcessor);
	IPipelineExtendersConfigurator registerXepProcessorFactory(IXepProcessorFactory<?, ?> xepProcessorFactory);
	
	IPipelineExtendersConfigurator registerCocTranslator(Class<?> protocolObjectType);
	IPipelineExtendersConfigurator registerTranslatorFactory(IProtocolTranslatorFactory<?> translatorFactory);

	IPipelineExtendersConfigurator registerPipelinePreprocessor(IPipelinePreprocessor pipelinePreprocessor);
	IPipelineExtendersConfigurator registerPipelinePostprocessor(IPipelinePostprocessor pipelinePostprocessor);
		
	<E extends IEvent> IPipelineExtendersConfigurator registerEventListener(Class<E> eventType, IEventListener<E> listener);
	IPipelineExtendersConfigurator registerEventListenerFactory(IEventListenerFactory<?> listenerFactory);
	
	IPipelineExtendersConfigurator registerIqResultProcessor(IIqResultProcessor iqResultProcessor);
	IPipelineExtendersConfigurator registerSessionListener(ISessionListener sessionListener);
}
