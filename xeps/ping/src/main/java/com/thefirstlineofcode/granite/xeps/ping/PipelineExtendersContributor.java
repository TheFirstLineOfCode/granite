package com.thefirstlineofcode.granite.xeps.ping;

import org.pf4j.Extension;

import com.thefirstlineofcode.basalt.xeps.ping.Ping;
import com.thefirstlineofcode.basalt.xmpp.core.IqProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.IPipelineExtendersConfigurator;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.PipelineExtendersConfigurator;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.parsing.SimpleObjectProtocolParserFactory;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.SimpleObjectProtocolTranslatorFactory;

@Extension
public class PipelineExtendersContributor extends PipelineExtendersConfigurator {	
	private static final ProtocolChain PROTOCOL_CHAIN = new IqProtocolChain(Ping.PROTOCOL);
	
	@Override
	protected void configure(IPipelineExtendersConfigurator configurator) {
		configurator.
			registerParserFactory(new SimpleObjectProtocolParserFactory<Ping>(PROTOCOL_CHAIN, Ping.class)).
			registerSingletonXepProcessor(PROTOCOL_CHAIN, new PingProcessor()).
			registerTranslatorFactory(new SimpleObjectProtocolTranslatorFactory<Ping>(Ping.class, Ping.PROTOCOL));
	}
}
