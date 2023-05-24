package com.thefirstlineofcode.granite.xeps.muc;

import com.thefirstlineofcode.basalt.xeps.muc.user.MucUser;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IXepProcessor;

public class MucUserPresenceProcessor implements IXepProcessor<Presence, MucUser> {
	@Dependency("muc.protocols.delegator")
	private MucProtocolsDelegator delegator;

	@Override
	public void process(IProcessingContext context, Presence presence, MucUser mucUser) {
		delegator.process(context, presence, mucUser);
	}

}
