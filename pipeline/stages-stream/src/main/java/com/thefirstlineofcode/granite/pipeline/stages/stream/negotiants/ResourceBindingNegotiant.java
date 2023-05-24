package com.thefirstlineofcode.granite.pipeline.stages.stream.negotiants;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.oxm.IOxmFactory;
import com.thefirstlineofcode.basalt.oxm.OxmService;
import com.thefirstlineofcode.basalt.oxm.annotation.AnnotatedParserFactory;
import com.thefirstlineofcode.basalt.oxm.parsers.core.stanza.IqParserFactory;
import com.thefirstlineofcode.basalt.oxm.parsers.core.stream.BindParser;
import com.thefirstlineofcode.basalt.oxm.translators.core.stanza.IqTranslatorFactory;
import com.thefirstlineofcode.basalt.oxm.translators.core.stream.BindTranslatorFactory;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.MalformedJidException;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.BadRequest;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.NotAuthorized;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Bind;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Stream;
import com.thefirstlineofcode.basalt.xmpp.core.stream.error.Conflict;
import com.thefirstlineofcode.basalt.xmpp.core.stream.error.InternalServerError;
import com.thefirstlineofcode.granite.framework.core.connection.IClientConnectionContext;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionManager;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing.IRouter;
import com.thefirstlineofcode.granite.framework.core.session.ISessionManager;
import com.thefirstlineofcode.granite.pipeline.stages.stream.StreamConstants;

public class ResourceBindingNegotiant extends AbstractNegotiant {
	private static final Logger logger = LoggerFactory.getLogger(ResourceBindingNegotiant.class);
	
	protected static IOxmFactory oxmFactory = OxmService.createStreamOxmFactory();
	
	private static final String STRING_CONFLICT = oxmFactory.getTranslatingFactory().translate(new Conflict());
	private static final String STRING_CLOSE_STREAM = oxmFactory.getTranslatingFactory().translate(new Stream(true));
	
	static {
		oxmFactory.register(ProtocolChain.first(Iq.PROTOCOL),
				new IqParserFactory()
		);
		oxmFactory.register(ProtocolChain.first(Iq.PROTOCOL).next(Bind.PROTOCOL),
				new AnnotatedParserFactory<>(BindParser.class)
		);
		
		oxmFactory.register(Iq.class, new IqTranslatorFactory());
		oxmFactory.register(Bind.class, new BindTranslatorFactory());
	}
	
	protected IConnectionManager connectionManager;
	protected ISessionManager sessionManager;
	protected String domainName;
	protected IRouter router;
	
	public ResourceBindingNegotiant(IConnectionManager connectionManager, String domainName,
			ISessionManager sessionManager, IRouter router) {
		this.connectionManager = connectionManager;
		this.domainName = domainName;
		this.sessionManager = sessionManager;
		this.router = router;
	}

	@Override
	protected boolean doNegotiate(IClientConnectionContext context, IMessage message) {
		Object request = oxmFactory.parse((String)message.getPayload());
		if (request instanceof Iq) {
			Iq iq = (Iq)request;
			if (iq.getObject() instanceof Bind) {
				Bind bind = iq.getObject();
				
				String authorizationId = context.removeAttribute(StreamConstants.KEY_AUTHORIZATION_ID);
				JabberId jid;
				if (bind.getResource() != null) {
					jid = specifiedByClient(authorizationId, bind.getResource());
				} else {
					jid = generatedByServer(authorizationId);
				}
				
				Iq response = new Iq(Iq.Type.RESULT);
				response.setId(iq.getId());
				bind = new Bind(jid);
				response.setObject(bind);
				
				context.setAttribute(StreamConstants.KEY_BINDED_JID, jid);
				context.write(oxmFactory.translate(response));
			} else {
				throw new ProtocolException(new NotAuthorized("Not a resource binding request."));
			}
		} else {
			throw new ProtocolException(new NotAuthorized("Not a resource binding request."));
		}
		
		return true;
	}

	protected JabberId generatedByServer(String authorizationId) {
		JabberId jid = null;
		for (int i = 0; i < 10; i++) {
			String resource = Long.toHexString(UUID.randomUUID().getLeastSignificantBits()).substring(4, 12);
			jid = JabberId.parse(String.format("%s@%s/%s", authorizationId, domainName, resource));
			
			if (!sessionManager.exists(jid)) {
				break;
			}
			
			jid = null;
		}
		
		if (jid == null) {
			throw new ProtocolException(new InternalServerError("Failed to generate a resource for binding."));
		}
		
		return jid;
	}

	protected JabberId specifiedByClient(String authorizationId, String resource) {
		JabberId jid;
		try {
			jid = JabberId.parse(String.format("%s@%s/%s", authorizationId, domainName, resource));
		} catch (MalformedJidException e) {
			throw new ProtocolException(new BadRequest(String.format("Invalid binding resource: %s.", resource)), e);
		}
		
		if (sessionManager.exists(jid)) {
			IClientConnectionContext connectionContext = (IClientConnectionContext)connectionManager.getConnectionContext(jid);
			connectionContext.write(STRING_CONFLICT);
			connectionContext.write(STRING_CLOSE_STREAM);
			connectionContext.close(true);
			
			try {
				router.unregister(jid);
			} catch (Exception e) {
				logger.error(String.format("Can't unregister session %s from router.", jid), e);
			}
			
			try {
				sessionManager.remove(jid);
			} catch (Exception e) {
				logger.error(String.format("Can't remove session %s.", jid), e);
			}
		}
		
		return jid;
	}

}
