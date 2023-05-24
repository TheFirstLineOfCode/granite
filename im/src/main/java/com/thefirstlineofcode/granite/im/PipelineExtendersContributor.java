package com.thefirstlineofcode.granite.im;

import org.pf4j.Extension;

import com.thefirstlineofcode.basalt.oxm.annotation.AnnotatedParserFactory;
import com.thefirstlineofcode.basalt.xmpp.core.IqProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.im.roster.Roster;
import com.thefirstlineofcode.basalt.xmpp.im.roster.RosterParser;
import com.thefirstlineofcode.basalt.xmpp.im.roster.RosterTranslatorFactory;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.IPipelineExtendersConfigurator;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.PipelineExtendersConfigurator;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.parsing.ProtocolParserFactory;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.ProtocolTranslatorFactory;
import com.thefirstlineofcode.granite.framework.im.ResourceAvailabledEvent;
import com.thefirstlineofcode.granite.framework.im.SessionListener;

@Extension
public class PipelineExtendersContributor extends PipelineExtendersConfigurator {
	
	@Override
	protected void configure(IPipelineExtendersConfigurator configurator) {
		configurator.registerParserFactory(new ProtocolParserFactory<>(
				new IqProtocolChain(Roster.PROTOCOL),
				new AnnotatedParserFactory<Roster>(RosterParser.class))
		);
		configurator.registerSingletonXepProcessor(new IqProtocolChain().next(Roster.PROTOCOL),
				new RosterProcessor());
		configurator.registerTranslatorFactory(
				new ProtocolTranslatorFactory<>(Roster.class, new RosterTranslatorFactory())
		);
		configurator.registerEventListener(
				ResourceAvailabledEvent.class, new ResourceAvailabledEventListener()
		);
		configurator.registerSessionListener(new SessionListener());
	}
}
