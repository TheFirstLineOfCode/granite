package com.thefirstlineofcode.granite.xeps.ping;

import com.thefirstlineofcode.basalt.xeps.ping.Ping;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.BadRequest;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.NotAllowed;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.ServiceUnavailable;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.StanzaError;
import com.thefirstlineofcode.granite.framework.core.config.IConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IIqResultProcessor;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IXepProcessor;

public class PingProcessor implements IXepProcessor<Iq, Ping>, IServerConfigurationAware,
			IConfigurationAware, IIqResultProcessor {
	private static final String CONFIG_KEY_DISABLED = "disabled";
	private boolean disabled;
	private JabberId domain;
	
	@Override
	public void process(IProcessingContext context, Iq iq, Ping ping) {
		if (disabled) {
			ServiceUnavailable error = StanzaError.create(iq, ServiceUnavailable.class);
			context.write(error);
		} else if (isC2SPing(iq)) {
			Iq pong = new Iq(Iq.Type.RESULT, iq.getId());
			context.write(pong);
		} else if (isC2CPing(iq)) {
			deliverPing(context, iq);
		} else {
			throw new ProtocolException(new BadRequest("Unsupported ping mode."));
		}
	}

	private void deliverPing(IProcessingContext context, Iq iq) {
		context.write(iq);
	}

	private boolean isC2CPing(Iq iq) {
		if (iq.getTo() == null || iq.getTo().equals(domain))
			return false;
		
		if (iq.getTo().isBareId()) {
			throw new ProtocolException(new NotAllowed("Can't ping bare JID."));
		}
		
		return true;
	}

	private boolean isC2SPing(Iq iq) {
		return iq.getType() == Iq.Type.GET && (iq.getTo() == null || iq.getTo().equals(domain));
	}

	@Override
	public void setConfiguration(IConfiguration configuration) {
		disabled = configuration.getBoolean(CONFIG_KEY_DISABLED, false);
	}

	@Override
	public void setServerConfiguration(IServerConfiguration serverConfiguration) {
		domain = JabberId.parse(serverConfiguration.getDomainName());
	}

	@Override
	public boolean processResult(IProcessingContext context, Iq result) {
		context.write(result);
		return true;
	}

	@Override
	public boolean processError(IProcessingContext context, StanzaError error) {
		context.write(error);
		return true;
	}
}
