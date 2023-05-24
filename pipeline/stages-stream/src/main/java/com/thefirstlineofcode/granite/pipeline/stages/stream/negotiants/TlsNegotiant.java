package com.thefirstlineofcode.granite.pipeline.stages.stream.negotiants;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.oxm.annotation.AnnotatedParserFactory;
import com.thefirstlineofcode.basalt.oxm.parsers.core.stream.tls.StartTlsParser;
import com.thefirstlineofcode.basalt.oxm.translators.SimpleObjectTranslatorFactory;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Feature;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Stream;
import com.thefirstlineofcode.basalt.xmpp.core.stream.error.NotAuthorized;
import com.thefirstlineofcode.basalt.xmpp.core.stream.tls.Failure;
import com.thefirstlineofcode.basalt.xmpp.core.stream.tls.Proceed;
import com.thefirstlineofcode.basalt.xmpp.core.stream.tls.StartTls;
import com.thefirstlineofcode.granite.framework.core.connection.IClientConnectionContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;

public class TlsNegotiant extends InitialStreamNegotiant {
	private static final Logger logger = LoggerFactory.getLogger(TlsNegotiant.class);
	
	static {
		oxmFactory.register(ProtocolChain.first(StartTls.PROTOCOL),
				new AnnotatedParserFactory<>(StartTlsParser.class));
		
		oxmFactory.register(Proceed.class,
				new SimpleObjectTranslatorFactory<>(
						Proceed.class,
						Proceed.PROTOCOL)
				);
		oxmFactory.register(Failure.class,
				new SimpleObjectTranslatorFactory<>(
						Failure.class,
						Failure.PROTOCOL)
				);
		
	}
	
	private enum TlsNegotiationState {
		NONE,
		PROCEED_RESPONSE_SENT,
		TLS_ESTABLISHED
	};
	
	private boolean tlsRequired;
	private TlsNegotiationState state;
	
	public TlsNegotiant(String domainName, boolean tlsRequired, List<Feature> features) {
		super(domainName, features);
		
		this.tlsRequired = tlsRequired;
		state = TlsNegotiationState.NONE;
	}

	@Override
	protected boolean doNegotiate(IClientConnectionContext context, IMessage message) {
		if (!context.isTlsSupported()) {
			throw new RuntimeException("TLS not supported.");
		}
		
		Object request = null;
		try {
			request = oxmFactory.parse((String)message.getPayload(), true);
		} catch (ProtocolException e) {
			if (tlsRequired) {
				throw e;
			} else {
				if (next != null) {
					done = true;
					return next.negotiate(context, message);
				} else {
					logger.warn("Is TLS negotiant the last negotiant???");
					throw new RuntimeException("IS TLS negotiant the last negotiant???");
				}
			}
		}
		
		if (state != TlsNegotiationState.TLS_ESTABLISHED && isTlsStarted(context)) {
			state = TlsNegotiationState.TLS_ESTABLISHED;
		}
		
		if (state == TlsNegotiationState.NONE) {
			if (request instanceof StartTls) {
				processStartTlsRequest(context);
				return false;
			} else {
				throw new ProtocolException(new NotAuthorized());
			}			
		} else {
			return super.doNegotiate(context, message);
		}
	}

	protected boolean isTlsStarted(IClientConnectionContext context) {
		return context.isTlsSupported() && context.isTlsStarted();
	}

	private void processStartTlsRequest(IClientConnectionContext context) {
		if (!context.isTlsSupported()) {
			processFailure(context);
			return;
		}
		
		startTls(context);
		context.write(oxmFactory.translate(new Proceed()));
		state = TlsNegotiationState.PROCEED_RESPONSE_SENT;
	}

	private void processFailure(IClientConnectionContext context) {
		Failure failure = new Failure();
		context.write(oxmFactory.translate(failure));
		
		context.write(oxmFactory.translate(new Stream(true)));
		
		context.close(true);
	}

	protected void startTls(IClientConnectionContext context) {
		try {
			context.startTls();
		} catch (SecurityException e) {
			processFailure(context);
			logger.error("Can't start TLS.", e);
		}
	}

}
