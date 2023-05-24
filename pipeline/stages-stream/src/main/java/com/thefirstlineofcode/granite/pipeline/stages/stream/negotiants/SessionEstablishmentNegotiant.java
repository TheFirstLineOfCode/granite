package com.thefirstlineofcode.granite.pipeline.stages.stream.negotiants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.oxm.IOxmFactory;
import com.thefirstlineofcode.basalt.oxm.OxmService;
import com.thefirstlineofcode.basalt.oxm.parsers.SimpleObjectParserFactory;
import com.thefirstlineofcode.basalt.oxm.parsers.core.stanza.IqParserFactory;
import com.thefirstlineofcode.basalt.oxm.translators.core.stanza.IqTranslatorFactory;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.Conflict;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.Forbidden;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.InternalServerError;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.NotAuthorized;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Session;
import com.thefirstlineofcode.granite.framework.core.connection.IClientConnectionContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventFirer;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.SessionEstablishedEvent;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.IRouter;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.RoutingRegistrationException;
import com.thefirstlineofcode.granite.framework.core.session.ISession;
import com.thefirstlineofcode.granite.framework.core.session.ISessionListener;
import com.thefirstlineofcode.granite.framework.core.session.ISessionManager;
import com.thefirstlineofcode.granite.framework.core.session.SessionExistsException;
import com.thefirstlineofcode.granite.pipeline.stages.stream.StreamConstants;

public class SessionEstablishmentNegotiant extends AbstractNegotiant {
	private static final Logger logger = LoggerFactory.getLogger(SessionEstablishmentNegotiant.class);
	
	private static IOxmFactory oxmFactory = OxmService.createStreamOxmFactory();
	
	static {
		oxmFactory.register(ProtocolChain.first(Iq.PROTOCOL),
				new IqParserFactory()
		);
		oxmFactory.register(ProtocolChain.first(Iq.PROTOCOL).next(Session.PROTOCOL),
				new SimpleObjectParserFactory<>(
						Session.PROTOCOL,
						Session.class
				)
		);
		
		oxmFactory.register(Iq.class, new IqTranslatorFactory());
	}
	
	private IRouter router;
	private ISessionManager sessionManager;
	private IEventFirer eventFirer;
	private ISessionListener sessionListener;
	
	public SessionEstablishmentNegotiant(IRouter router, ISessionManager sessionManager,
			IEventFirer eventFirer, ISessionListener sessionListener) {
		this.sessionManager = sessionManager;
		this.router = router;
		this.eventFirer = eventFirer;
		this.sessionListener = sessionListener;
	}
	
	@Override
	protected boolean doNegotiate(IClientConnectionContext context, IMessage message) {
		Iq request = (Iq)oxmFactory.parse((String)message.getPayload());
		
		if (request.getObject() instanceof Session) {
			JabberId sessionJid = context.removeAttribute(StreamConstants.KEY_BINDED_JID);
			if (sessionJid == null) {
				throw new ProtocolException(new Forbidden());
			}
			
			try {
				sessionListener.sessionEstablishing(context, sessionJid);
			} catch (Exception e) {
				logger.error(String.format("Failed to call sessionEstablishing() of session listeners. JID: %s.",
						sessionJid), e);
				throw new ProtocolException(new InternalServerError(e.getMessage()));
			}
			
			
			ISession session = null;
			try {
				session = sessionManager.create(sessionJid);
			} catch (SessionExistsException e) {
				// TODO Maybe we should remove previous session and disconnect the associated client.
				throw new ProtocolException(new Conflict(String.format("Session '%s' has already existed.")));
			}
			
			context.setAttribute(StreamConstants.KEY_SESSION_JID, sessionJid);
			session.setAttribute(StreamConstants.KEY_CLIENT_SESSION_ID,
					context.getConnectionId());
			session.setAttribute(ISession.KEY_SESSION_JID, sessionJid);
			
			sessionManager.put(sessionJid, session);
			
			try {
				router.register(sessionJid, context.getLocalNodeId());
				if (logger.isDebugEnabled())
					logger.debug("Session[{}] registered on local node[{}].", sessionJid, context.getLocalNodeId());
			} catch (RoutingRegistrationException e) {
				logger.error(String.format("Can't register to router. JID: %s.", sessionJid), e);
				sessionManager.remove(sessionJid);
				throw new ProtocolException(new InternalServerError(e.getMessage()));
			}
			
			try {
				sessionListener.sessionEstablished(context, sessionJid);
			} catch (Exception e) {
				logger.error(String.format("Failed to call sessionEstablished() of session listeners. JID: %s.",
						sessionJid), e);
				try {
					router.unregister(sessionJid);
				} catch (RoutingRegistrationException e1) {
					logger.error("Can't unregister from router. JID: {}.", sessionJid);
				}
				sessionManager.remove(sessionJid);
				
				throw new ProtocolException(new InternalServerError(e.getMessage()));
			}
			
			
			fireSessionEstablishedEvent(context, sessionJid);
			
			Iq response = new Iq(Iq.Type.RESULT);
			response.setId(request.getId());
			
			context.write(oxmFactory.translate(response));
			
			return true;
		} else {
			throw new ProtocolException(new NotAuthorized());
		}
	}
	
	private void fireSessionEstablishedEvent(IClientConnectionContext context, JabberId jid) {
		SessionEstablishedEvent event = new SessionEstablishedEvent(context.getConnectionId().toString(), jid);
		
		eventFirer.fire(event);
	}

}
