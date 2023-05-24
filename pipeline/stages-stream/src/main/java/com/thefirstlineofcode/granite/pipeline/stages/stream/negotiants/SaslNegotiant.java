package com.thefirstlineofcode.granite.pipeline.stages.stream.negotiants;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

import org.bouncycastle.util.encoders.Base64;

import com.thefirstlineofcode.basalt.oxm.IOxmFactory;
import com.thefirstlineofcode.basalt.oxm.OxmService;
import com.thefirstlineofcode.basalt.oxm.annotation.AnnotatedParserFactory;
import com.thefirstlineofcode.basalt.oxm.parsers.SimpleObjectParserFactory;
import com.thefirstlineofcode.basalt.oxm.parsers.core.stream.sasl.AuthParser;
import com.thefirstlineofcode.basalt.oxm.translators.SimpleObjectTranslatorFactory;
import com.thefirstlineofcode.basalt.oxm.translators.core.stream.BindTranslatorFactory;
import com.thefirstlineofcode.basalt.oxm.translators.core.stream.sasl.FailureTranslatorFactory;
import com.thefirstlineofcode.basalt.xmpp.Constants;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Bind;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Feature;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Session;
import com.thefirstlineofcode.basalt.xmpp.core.stream.error.NotAuthorized;
import com.thefirstlineofcode.basalt.xmpp.core.stream.sasl.Abort;
import com.thefirstlineofcode.basalt.xmpp.core.stream.sasl.Auth;
import com.thefirstlineofcode.basalt.xmpp.core.stream.sasl.Challenge;
import com.thefirstlineofcode.basalt.xmpp.core.stream.sasl.Failure;
import com.thefirstlineofcode.basalt.xmpp.core.stream.sasl.Response;
import com.thefirstlineofcode.basalt.xmpp.core.stream.sasl.Success;
import com.thefirstlineofcode.granite.framework.core.auth.IAuthenticator;
import com.thefirstlineofcode.granite.framework.core.connection.IClientConnectionContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.pipeline.stages.stream.StreamConstants;

public class SaslNegotiant extends InitialStreamNegotiant {
	private static IOxmFactory oxmFactory = OxmService.createStreamOxmFactory();
	
	private enum NegotiationState {
		NONE,
		AUTH_REQUEST_RECEIVED,
		CHALLENGE_SENT,
		RESPONSE_RECEIVED,
		ABORTED,
		FAILED,
		SUCCESSFUL
	};
	
	static {
		InitialStreamNegotiant.oxmFactory.register(Bind.class,
				new BindTranslatorFactory()
				);
		InitialStreamNegotiant.oxmFactory.register(Session.class,
				new SimpleObjectTranslatorFactory<>(
						Session.class,
						Session.PROTOCOL)
				);
		
		oxmFactory.register(ProtocolChain.first(Auth.PROTOCOL),
				new AnnotatedParserFactory<>(AuthParser.class)
				);
		oxmFactory.register(ProtocolChain.first(Response.PROTOCOL),
				new SimpleObjectParserFactory<>(
						Response.PROTOCOL,
						Response.class)
				);
		oxmFactory.register(ProtocolChain.first(Abort.PROTOCOL),
				new SimpleObjectParserFactory<>(
						Abort.PROTOCOL,
						Abort.class)
				);
		
		oxmFactory.register(Challenge.class,
				new SimpleObjectTranslatorFactory<>(
						Challenge.class,
						Challenge.PROTOCOL)
				);
		oxmFactory.register(Failure.class,
				new FailureTranslatorFactory()
				);
		oxmFactory.register(Success.class,
				new SimpleObjectTranslatorFactory<>(
						Success.class,
						Success.PROTOCOL)
				);
	}
	
	private NegotiationState state;
	private String[] supportedMechanisms;
	private int abortRetries;
	private int failureRetries;
	private SaslServer saslServer;
	private String domainName;
	private IAuthenticator autenticator;
	
	public SaslNegotiant(String domainName, String[] supportedMechanisms, int abortRetries, int failureRetries,
			List<Feature> features, IAuthenticator autenticator) {
		super(domainName, features);
		
		state = NegotiationState.NONE;
		this.supportedMechanisms = supportedMechanisms;
		this.abortRetries = abortRetries;
		this.failureRetries = failureRetries;
		this.domainName = domainName;
		this.autenticator = autenticator;
	}

	@Override
	protected boolean doNegotiate(IClientConnectionContext context, IMessage message) {
		try {
			if (state != NegotiationState.SUCCESSFUL) {
				saslNegotiate(context, (String)message.getPayload());
				return false;
			} else {
				return super.doNegotiate(context, message);
			}
			
		} catch (Exception e) {
			processFailure(context, Failure.ErrorCondition.TEMPORARY_AUTH_FAILURE);
			return false;
		}
	}
	
	private void processFailure(IClientConnectionContext context, Failure.ErrorCondition errorCondition) {
		context.write(oxmFactory.translate(new Failure(errorCondition)));
		
		if (failureRetries == 0) {
			closeStream(context, oxmFactory);
		} else {
			failureRetries--;
		}
		
		state = NegotiationState.FAILED;
	}

	private void saslNegotiate(IClientConnectionContext context, String message) {
		Object object = oxmFactory.parse(message, true);
		if (object instanceof Abort) {
			processAbort(context);
			return;
		}
		
		if (state == NegotiationState.NONE ||
				state == NegotiationState.ABORTED ||
					state == NegotiationState.FAILED) {
			String mechanism = processAuth(context, object, message);
			
			if (!isMechanismSupported(mechanism)) {
				processFailure(context, Failure.ErrorCondition.INVALID_MECHANISM);
				return;
			}
			
			state = NegotiationState.AUTH_REQUEST_RECEIVED;
			try {
				Map<String, Object> properties = new HashMap<>();
				properties.put(Sasl.QOP, "auth");
				saslServer = Sasl.createSaslServer(mechanism, Constants.PROTOCOL_NAME, domainName, properties,
						new DefaultAuthenticationCallbackHandler(autenticator));
				String challengeMessage = createChallengeMessage(new byte[0]);
				if (challengeMessage != null)
					context.write(challengeMessage);
				state = NegotiationState.CHALLENGE_SENT;
			} catch (SaslException e) {
				processFailure(context, Failure.ErrorCondition.TEMPORARY_AUTH_FAILURE);
				return;
			} catch (UnsupportedEncodingException e) {
				processFailure(context, Failure.ErrorCondition.INCORRECT_ENCODING);
				return;
			}
		} else if (state == NegotiationState.CHALLENGE_SENT) {
			if (!(object instanceof Response)) {
				processFailure(context, Failure.ErrorCondition.NOT_AUTHORIZED);
				return;
			}
			
			byte[] response = null;
			try {
				response = processResponse((Response)object);
			} catch (UnsupportedEncodingException e) {
				processFailure(context, Failure.ErrorCondition.INCORRECT_ENCODING);
				return;
			}
			
			state = NegotiationState.RESPONSE_RECEIVED;
			String challengeMessage = null;
			try {
				challengeMessage = createChallengeMessage(response);
				if(challengeMessage == null){
					state = NegotiationState.SUCCESSFUL;
					context.setAttribute(StreamConstants.KEY_AUTHORIZATION_ID, saslServer.getAuthorizationID());
					context.write(oxmFactory.translate(new Success()));
					return;
				} else {
					context.write(challengeMessage);
					state = NegotiationState.CHALLENGE_SENT;
				}
			} catch (SaslException e) {
				processFailure(context, Failure.ErrorCondition.NOT_AUTHORIZED);
				return;
			} catch (UnsupportedEncodingException e) {
				processFailure(context, Failure.ErrorCondition.TEMPORARY_AUTH_FAILURE);
				return;
			}
			return;
		}
	}

	private boolean isMechanismSupported(String mechanism) {
		for (String aMechanism : supportedMechanisms) {
			if (aMechanism.equals(mechanism))
				return true;
		}
		
		return false;
	}

	private void processAbort(IClientConnectionContext context) {
		context.write(oxmFactory.translate(new Failure(Failure.ErrorCondition.ABORTED)));
		if (abortRetries == 0) {
			closeStream(context, oxmFactory);
		} else {
			abortRetries--;
		}
		state = NegotiationState.ABORTED;
	}

	private String processAuth(IClientConnectionContext context, Object object, String message) {
		if (object instanceof Auth) {
			return ((Auth)object).getMechanism();
		} else {
			throw new ProtocolException(new NotAuthorized(String.format("Not authorized. Message: %s.", object, message)));
		}
	}

	private String createChallengeMessage(byte[] response) throws SaslException, UnsupportedEncodingException {
		if (saslServer.isComplete())
			return null;
		
		byte[] challenge = saslServer.evaluateResponse(response);
		if(challenge!=null){
			String encodedChallenge = new String(Base64.encode(challenge), "utf-8");
			return oxmFactory.translate(new Challenge(encodedChallenge));
		}
		return null;
	}

	private byte[] processResponse(Response response) throws UnsupportedEncodingException {
		if (response.getText() != null)
			return Base64.decode(response.getText().getBytes("utf-8"));
		
		return new byte[0];
	}
	
}
