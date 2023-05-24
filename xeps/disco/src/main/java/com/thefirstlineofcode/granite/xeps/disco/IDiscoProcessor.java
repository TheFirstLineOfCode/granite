package com.thefirstlineofcode.granite.xeps.disco;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;

public interface IDiscoProcessor {
	void discoInfo(IProcessingContext context, Iq iq, JabberId jid, String node);
	void discoItems(IProcessingContext context, Iq iq, JabberId jid, String node);
}
