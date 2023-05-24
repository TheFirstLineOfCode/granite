package com.thefirstlineofcode.granite.im;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.BadRequest;
import com.thefirstlineofcode.basalt.xmpp.im.roster.Roster;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IXepProcessor;
import com.thefirstlineofcode.granite.framework.core.session.ISession;

public class RosterProcessor implements IXepProcessor<Iq, Roster> {
	
	@Dependency("roster.operator")
	private RosterOperator rosterOperator;

	@Override
	public void process(IProcessingContext context, Iq iq, Roster roster) {
		JabberId userJid = context.getAttribute(ISession.KEY_SESSION_JID);
		
		if (iq.getType() == Iq.Type.SET) {
			rosterOperator.rosterSet(context, userJid, roster);
			rosterOperator.reply(context, userJid, iq.getId());
		} else if (iq.getType() == Iq.Type.GET) {
			rosterOperator.rosterGet(context, userJid, iq.getId());
		} else {
			throw new ProtocolException(new BadRequest("Roster result not supported."));
		}
		
	}

}
