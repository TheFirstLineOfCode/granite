package com.thefirstlineofcode.granite.stream.standard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.oxm.OxmService;
import com.thefirstlineofcode.basalt.oxm.annotation.AnnotatedParserFactory;
import com.thefirstlineofcode.basalt.oxm.parsers.core.stream.StreamParser;
import com.thefirstlineofcode.basalt.oxm.parsing.IParsingFactory;
import com.thefirstlineofcode.basalt.oxm.translating.ITranslatingFactory;
import com.thefirstlineofcode.basalt.oxm.translators.core.stream.StreamTranslatorFactory;
import com.thefirstlineofcode.basalt.oxm.translators.error.StanzaErrorTranslatorFactory;
import com.thefirstlineofcode.basalt.oxm.translators.error.StreamErrorTranslatorFactory;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.LangText;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.InternalServerError;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.StanzaError;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Bind;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Feature;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Session;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Stream;
import com.thefirstlineofcode.basalt.xmpp.core.stream.error.StreamError;
import com.thefirstlineofcode.basalt.xmpp.core.stream.sasl.Mechanisms;
import com.thefirstlineofcode.basalt.xmpp.core.stream.tls.StartTls;
import com.thefirstlineofcode.granite.framework.adf.core.AdfComponentService;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentServiceAware;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.auth.IAuthenticator;
import com.thefirstlineofcode.granite.framework.core.config.IConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.connection.IClientConnectionContext;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionManager;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionManagerAware;
import com.thefirstlineofcode.granite.framework.core.pipeline.IClientMessageProcessor;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageChannel;
import com.thefirstlineofcode.granite.framework.core.pipeline.SimpleMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.IPipelineExtendersContributor;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.ConnectionClosedEvent;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.ConnectionOpenedEvent;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventFirer;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventFirerAware;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.IRouter;
import com.thefirstlineofcode.granite.framework.core.repository.IInitializable;
import com.thefirstlineofcode.granite.framework.core.session.ISessionListener;
import com.thefirstlineofcode.granite.framework.core.session.ISessionManager;
import com.thefirstlineofcode.granite.framework.core.utils.CommonUtils;
import com.thefirstlineofcode.granite.pipeline.stages.stream.IStreamNegotiant;
import com.thefirstlineofcode.granite.pipeline.stages.stream.StreamConstants;
import com.thefirstlineofcode.granite.pipeline.stages.stream.negotiants.InitialStreamNegotiant;
import com.thefirstlineofcode.granite.pipeline.stages.stream.negotiants.ResourceBindingNegotiant;
import com.thefirstlineofcode.granite.pipeline.stages.stream.negotiants.SaslNegotiant;
import com.thefirstlineofcode.granite.pipeline.stages.stream.negotiants.SessionEstablishmentNegotiant;
import com.thefirstlineofcode.granite.pipeline.stages.stream.negotiants.TlsNegotiant;

@Component("standard.client.message.processor")
public class StandardClientMessageProcessor implements IClientMessageProcessor, IConfigurationAware,
		IServerConfigurationAware, IApplicationComponentServiceAware, IEventFirerAware, IInitializable {
	private static final String BEAN_NAME_AUTHENTICATOR = "authenticator";

	private static final Logger logger = LoggerFactory.getLogger(StandardClientMessageProcessor.class);
	
	private static final String CONFIGURATION_KEY_TLS_REQUIRED = "tls.required";
	private static final String CONFIGURATION_KEY_SASL_FAILURE_RETRIES = "sasl.failure.retries";
	private static final String CONFIGURATION_KEY_SASL_ABORT_RETRIES = "sasl.abort.retries";
	private static final String CONFIGURATION_KEY_SASL_SUPPORTED_MECHANISMS = "sasl.supported.mechanisms";
	
	private static final Object KEY_NEGOTIANT = "granite.key.negotiant";
	
	protected IConnectionManager connectionManager;

	private IParsingFactory parsingFactory;
	private ITranslatingFactory translatingFactory;
	
	protected IAuthenticator authenticator;	
	protected ISessionManager sessionManager;
	protected IMessageChannel messageChannel;
	protected IEventFirer eventFirer;
	protected IRouter router;
	
	protected String hostName;
	
	protected boolean tlsRequired;
	
	protected int saslAbortRetries;
	protected int saslFailureRetries;
	protected String[] saslSupportedMechanisms;
	
	protected ISessionListener sessionListenerDelegate;
	
	protected List<ISessionListener> sessionListeners;
	
	protected IApplicationComponentService appComponentService;
	
	public StandardClientMessageProcessor() {
		parsingFactory = OxmService.createParsingFactory();
		parsingFactory.register(ProtocolChain.first(Stream.PROTOCOL), new AnnotatedParserFactory<>(StreamParser.class));
		
		translatingFactory = OxmService.createTranslatingFactory();
		translatingFactory.register(Stream.class, new StreamTranslatorFactory());
		translatingFactory.register(StreamError.class, new StreamErrorTranslatorFactory());
		translatingFactory.register(StanzaError.class, new StanzaErrorTranslatorFactory());
		
		sessionListeners = new ArrayList<>();
		sessionListenerDelegate = new SessionListenerDelegate();
	}
	
	private class SessionListenerDelegate implements ISessionListener {

		@Override
		public void sessionEstablishing(IConnectionContext context, JabberId sessionJid) throws Exception {
			for (ISessionListener sessionListener : sessionListeners) {
				sessionListener.sessionEstablishing(context, sessionJid);
			}
		}
		
		@Override
		public void sessionEstablished(IConnectionContext context, JabberId sessionJid) throws Exception {
			for (ISessionListener sessionListener : sessionListeners) {
				sessionListener.sessionEstablished(context, sessionJid);
			}
		}

		@Override
		public void sessionClosing(IConnectionContext context, JabberId sessionJid) throws Exception {
			for (ISessionListener sessionListener : sessionListeners) {
				sessionListener.sessionClosing(context, sessionJid);
			}
		}

		@Override
		public void sessionClosed(IConnectionContext context, JabberId sessionJid) throws Exception {
			for (ISessionListener sessionListener : sessionListeners) {
				sessionListener.sessionClosed(context, sessionJid);
			}
		}
		
	}

	@Override
	public void process(IConnectionContext context, IMessage message) {
		doProcess((IClientConnectionContext)context, message);
		
	}
	
	private void doProcess(IClientConnectionContext context, IMessage message) {
		if (isCloseStreamRequest((String)message.getPayload())) {
			context.write(translatingFactory.translate(new Stream(true)));
			context.close();
			return;
		}
				
		JabberId jid = context.getAttribute(StreamConstants.KEY_SESSION_JID);
		if (jid != null) {
			Map<Object, Object> headers = new HashMap<>();
			headers.put(IMessage.KEY_SESSION_JID, jid);
			IMessage out = new SimpleMessage(headers, message.getPayload());
			
			messageChannel.send(out);
		} else {
			IStreamNegotiant negotiant = context.getAttribute(KEY_NEGOTIANT);
			if (negotiant == null) {
				negotiant = createNegotiant();
				context.setAttribute(KEY_NEGOTIANT, negotiant);
				
				if (logger.isDebugEnabled()) {
					logger.debug("Begin to negotiate stream. Connection ID: {}.", context.getConnectionId());
				}
			}
			
			try {
				if (negotiant.negotiate(context, message)) {
					context.removeAttribute(KEY_NEGOTIANT);
					if (logger.isDebugEnabled()) {
						logger.debug("Stream has negotiated. Connection ID: {}, Session JID: {}.",
								context.getConnectionId(), context.getJid());
					}
				}
			} catch (ProtocolException e) {
				if (logger.isDebugEnabled())
					logger.debug("Failed to negotiate stream.", e);
				
				context.write(translatingFactory.translate(e.getError()));
				if (e.getError() instanceof StreamError) {
					closeStream(context);
				}
			} catch (RuntimeException e) {
				logger.error("Negotiation error.", e);
				
				InternalServerError error = new InternalServerError();
				error.setText(new LangText(String.format("Negotiation error. %s.",
						CommonUtils.getInternalServerErrorMessage(e))));
				context.write(translatingFactory.translate(error));
				closeStream(context);
			}
			
		}
	}

	private void fireConnectionOpenedEvent(IClientConnectionContext context) {
		ConnectionOpenedEvent event = new ConnectionOpenedEvent(context.getConnectionId().toString(),
				context.getRemoteIp(), context.getRemotePort());	
		eventFirer.fire(event);
	}
	
	private void fireConnectionClosedEvent(IClientConnectionContext context) {
		ConnectionClosedEvent event = new ConnectionClosedEvent(context.getConnectionId().toString(),
				context.getJid(), context.getStreamId());	
		eventFirer.fire(event);
	}

	private boolean isCloseStreamRequest(String message) {
		try {
			Object object = parsingFactory.parse(message, true);
			if (object instanceof Stream) {
				return ((Stream)object).isClose();
			}
			
			return false;			
		} catch (Exception e) {
			return false;
		}

	}

	private void closeStream(IConnectionContext context) {
		context.write(translatingFactory.translate(new Stream(true)));
		context.close();
	}

	protected IStreamNegotiant createNegotiant() {
		IStreamNegotiant intialStream = new InitialStreamNegotiant(hostName,
				getInitialStreamNegotiantAdvertisements());
		
		IStreamNegotiant tls = new TlsNegotiant(hostName, tlsRequired,
				getTlsNegotiantAdvertisements());
		
		IStreamNegotiant sasl = new SaslNegotiant(hostName,
				saslSupportedMechanisms, saslAbortRetries, saslFailureRetries,
				getSaslNegotiantFeatures(), authenticator);
		
		IStreamNegotiant resourceBinding = new ResourceBindingNegotiant(connectionManager,
				hostName, sessionManager, router);
		IStreamNegotiant sessionEstablishment = new SessionEstablishmentNegotiant(router,
				sessionManager, eventFirer, sessionListenerDelegate);
		
		resourceBinding.setNext(sessionEstablishment);
		sasl.setNext(resourceBinding);
		tls.setNext(sasl);
		intialStream.setNext(tls);
		
		return intialStream;
	}

	private String[] parseSupportedMechanisms(String sMechanisms) {
		StringTokenizer st = new StringTokenizer(sMechanisms, ",");
		
		if (st.countTokens() == 0) {
			throw new IllegalArgumentException(String.format("Can't determine supported sasl mechanisms: %s.", sMechanisms));
		}
		
		String[] mechanisms = new String[st.countTokens()];
		int i = 0;
		while (st.hasMoreTokens()) {
			mechanisms[i] = st.nextToken().trim();
			i++;
		}
		
		return mechanisms;
	}

	protected List<Feature> getSaslNegotiantFeatures() {
		List<Feature> features = new ArrayList<>();
		features.add(new Bind());
		features.add(new Session());
		
		return features;
	}

	protected List<Feature> getTlsNegotiantAdvertisements() {
		List<Feature> features = new ArrayList<>();
		
		Mechanisms mechanisms = new Mechanisms();
		for (String supportedMechanism : saslSupportedMechanisms) {
			mechanisms.getMechanisms().add(supportedMechanism);
		}
		
		features.add(mechanisms);
		
		return features;
	}

	protected List<Feature> getInitialStreamNegotiantAdvertisements() {
		List<Feature> features = new ArrayList<>();
		
		StartTls startTls = new StartTls();
		if (tlsRequired) {
			startTls.setRequired(true);
		}
		features.add(startTls);
		
		features.addAll(getTlsNegotiantAdvertisements());
		
		return features;
	}

	@Override
	public void setServerConfiguration(IServerConfiguration serverConfiguration) {
		hostName = serverConfiguration.getDomainName();
	}

	@Override
	public void setConfiguration(IConfiguration configuration) {
		tlsRequired = configuration.getBoolean(CONFIGURATION_KEY_TLS_REQUIRED, true);
		
		saslSupportedMechanisms = parseSupportedMechanisms(configuration.getString(
				CONFIGURATION_KEY_SASL_SUPPORTED_MECHANISMS, "DIGEST-MD5"));
		saslAbortRetries = configuration.getInteger(CONFIGURATION_KEY_SASL_ABORT_RETRIES, 3);
		saslFailureRetries = configuration.getInteger(CONFIGURATION_KEY_SASL_FAILURE_RETRIES, 3);
	}

	@Override
	public void connectionOpened(IClientConnectionContext context) {
		fireConnectionOpenedEvent(context);
	}
	
	@Override
	public void connectionClosing(IClientConnectionContext context) {
		try {
			sessionListenerDelegate.sessionClosing(context, context.getJid());
		} catch (Exception e) {
			logger.error("Some errors occurred in session closing callback method.", e);
		}
	}

	@Override
	public void connectionClosed(IClientConnectionContext context, JabberId sessionJid) {
		try {
			sessionListenerDelegate.sessionClosed(context, sessionJid);
		} catch (Exception e) {
			logger.error("Some errors occurred in session closed callback method.", e);
		}
		
		fireConnectionClosedEvent(context);
	}
	
	@Dependency("session.manager")
	public void setSessionManager(ISessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}
	
	@Dependency("message.channel")
	public void setMessageChannel(IMessageChannel messageChannel) {
		this.messageChannel = messageChannel;
	}
	
	@Dependency("router")
	public void setRouter(IRouter router) {
		this.router = router;
	}

	@Override
	public void init() {
		loadContributedSessionListeners();
	}
	
	private void loadContributedSessionListeners() {
		IPipelineExtendersContributor[] contributors = CommonUtils.getExtendersContributors(appComponentService);
		if (contributors == null || contributors.length == 0) {
			return;
		}
		
		for (IPipelineExtendersContributor contributor: contributors) {
			ISessionListener[] sessionListeners = contributor.getSessionListeners();
			if (sessionListeners == null || sessionListeners.length == 0)
				continue;
			
			for (ISessionListener sessionListener : sessionListeners) {
				if (sessionListener instanceof IConnectionManagerAware) {
					((IConnectionManagerAware)sessionListener).setConnectionManager(connectionManager);
				}
				
				this.sessionListeners.add(appComponentService.inject(sessionListener));
			}
		}
	}

	@Override
	public void setConnectionManager(IConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	@Override
	public void setApplicationComponentService(IApplicationComponentService appComponentService) {
		this.appComponentService = appComponentService;
		authenticator = ((AdfComponentService)appComponentService).getApplicationContext().
				getBean(BEAN_NAME_AUTHENTICATOR, IAuthenticator.class);
	}

	@Override
	public void setEventFirer(IEventFirer eventFirer) {
		this.eventFirer = eventFirer;
	}

}
