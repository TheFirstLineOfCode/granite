package com.thefirstlineofcode.granite.pipeline.stages.parsing;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.oxm.OxmService;
import com.thefirstlineofcode.basalt.oxm.parsers.core.stanza.IqParserFactory;
import com.thefirstlineofcode.basalt.oxm.parsers.error.ErrorParserFactory;
import com.thefirstlineofcode.basalt.oxm.parsers.error.StanzaErrorDetailsParserFactory;
import com.thefirstlineofcode.basalt.oxm.parsing.FlawedProtocolObject;
import com.thefirstlineofcode.basalt.oxm.parsing.IParser;
import com.thefirstlineofcode.basalt.oxm.parsing.IParserFactory;
import com.thefirstlineofcode.basalt.oxm.parsing.IParsingFactory;
import com.thefirstlineofcode.basalt.xmpp.core.IError;
import com.thefirstlineofcode.basalt.xmpp.core.IqProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.Protocol;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.InternalServerError;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.ServiceUnavailable;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.StanzaError;
import com.thefirstlineofcode.basalt.xmpp.core.stream.error.InvalidFrom;
import com.thefirstlineofcode.basalt.xmpp.core.stream.error.StreamError;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentServiceAware;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.config.IConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageProcessor;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.IPipelineExtendersContributor;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.parsing.IPipelinePreprocessor;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.parsing.IProtocolParserFactory;
import com.thefirstlineofcode.granite.framework.core.repository.IInitializable;
import com.thefirstlineofcode.granite.framework.core.utils.CommonUtils;

@Component("minimum.message.parsing.processor")
public class MinimumMessageParsingProcessor implements IMessageProcessor, IInitializable,
		IConfigurationAware, IServerConfigurationAware, IApplicationComponentServiceAware {
	private static final String CONFIGURATION_KEY_STANZA_ERROR_ATTACH_SENDER_MESSAGE = "stanza.error.attach.sender.message";
	private static final char CHAR_HEART_BEAT = ' ';
	
	private static final Logger logger = LoggerFactory.getLogger(MinimumMessageParsingProcessor.class);
	
	protected IParsingFactory parsingFactory;
	private boolean stanzaErrorAttachSenderMessage;
	private IServerConfiguration serverConfiguration;
	
	private List<IPipelinePreprocessor> pipesPreprocessors;
	
	private IApplicationComponentService appComponentService;
	
	public MinimumMessageParsingProcessor() {
		parsingFactory = OxmService.createParsingFactory();
		pipesPreprocessors = new CopyOnWriteArrayList<>();
	}
	
	@Override
	public void init() {
		registerPredefinedParsers();
		
		IPipelineExtendersContributor[] extendersFactories = CommonUtils.getExtendersContributors(appComponentService);
		
		loadContributedProtocolParsers(extendersFactories);
		loadContributedPipelinePreprocessors(extendersFactories);
	}

	protected void loadContributedPipelinePreprocessors(IPipelineExtendersContributor[] extendersContributors) {
		for (IPipelineExtendersContributor extendersContributor : extendersContributors) {
			IPipelinePreprocessor[] pipelinePreproessors = extendersContributor.getPipelinePreprocessors();
			if (pipelinePreproessors == null || pipelinePreproessors.length == 0)
				continue;
			
			for (IPipelinePreprocessor pipelinePreprocessor : pipelinePreproessors) {
				pipesPreprocessors.add(appComponentService.inject(pipelinePreprocessor));
				
				if (logger.isDebugEnabled()) {
					logger.debug("Plugin '{}' contributed a pipeline preprocessor: '{}'.",
							appComponentService.getPluginManager().whichPlugin(extendersContributor.getClass()),
							pipelinePreprocessor.getClass().getName()
					);
				}
			}
		}
	}

	protected void loadContributedProtocolParsers(IPipelineExtendersContributor[] extendersContributors) {
		for (IPipelineExtendersContributor extendersContributor : extendersContributors) {
			IProtocolParserFactory<?>[] parserFactories = extendersContributor.getProtocolParserFactories();
			
			if (parserFactories == null || parserFactories.length == 0)
				continue;
			
			for (IProtocolParserFactory<?> parserFactory : parserFactories) {
				parsingFactory.register(parserFactory.getProtocolChain(), createParserFactoryAdapter(parserFactory));
				
				if (logger.isDebugEnabled()) {
					logger.debug("Plugin '{}' contributed a protocol parser factory: '{}'.",
							appComponentService.getPluginManager().whichPlugin(extendersContributor.getClass()),
							parserFactory.getClass().getName()
					);
				}
			}
		}
	}
	
	private <T> IParserFactory<T> createParserFactoryAdapter(IProtocolParserFactory<T> original) {
		return new ParserFactoryAdapter<>(original);
	}

	private class ParserFactoryAdapter<T> implements IParserFactory<T> {
		private IProtocolParserFactory<T> original;
		
		public ParserFactoryAdapter(IProtocolParserFactory<T> original) {
			this.original = original;
		}

		@Override
		public Protocol getProtocol() {
			return original.getProtocolChain().get(original.getProtocolChain().size() - 1);
		}

		@Override
		public IParser<T> create() {
			return original.createParser();
		}
		
	}

	protected void registerPredefinedParsers() {
		parsingFactory.register(
				new IqProtocolChain(),
				new IqParserFactory()
		);
		parsingFactory.register(
				ProtocolChain.first(StanzaError.PROTOCOL),
				new ErrorParserFactory<>(IError.Type.STANZA)
		);
		parsingFactory.register(
				ProtocolChain.first(StanzaError.PROTOCOL).next(
						StanzaError.PROTOCOL_ERROR_DEFINED_CONDITION),
				new StanzaErrorDetailsParserFactory()
		);
	}
	
	@Override
	public void process(IConnectionContext context, IMessage message) {
		String msg = (String)message.getPayload();
		
		if (logger.isDebugEnabled())
			logger.debug("Begin to parse the XMPP message. Session JID: '{}'. XMPP message: '{}'.", context.getJid(), msg);
		
		// Do nothing. We just need the program runs to here to touch the session.
		if (isHeartbeats(msg))
			return;
		
		String originalMsg = msg;
		Object out = null;
		try {
			out = doProcess(context, msg);
			
			if (out instanceof StreamError) {
				logger.warn("Received a stream error. We will close the stream. Session JID: '{}'. XMPP message: '{}'.",
						context.getJid(), originalMsg);
				
				context.close();
			} else {
				context.write(out);
				
				if (logger.isDebugEnabled())
					logger.debug("End of parsing the XMPP message. Session JID: '{}'. XMPP message: '{}'.", context.getJid(), originalMsg);
			}
		} catch (ProtocolException e) {
			IError error = e.getError();
			if ((error instanceof StanzaError)) {
				amendStanzaError(out, (StanzaError)error);
			}
			
			if (logger.isTraceEnabled())
				logger.trace("Parsing protocol exception. Session JID: '{}'. XMPP message: '{}'.",
						context.getJid(), message);
			
			context.write(error);
		} catch (RuntimeException e) {
			StanzaError error = new InternalServerError(CommonUtils.getInternalServerErrorMessage(e));
			amendStanzaError(out, error);
			
			logger.error(String.format("Parsing internal server error. Session JID: '{}'. XMPP message: '{}'.",
					context.getJid(), message), e);
			
			context.write(error);
		}
		
	}

	private Object doProcess(IConnectionContext context, String msg) {
		String originalMsg = msg;
		for (IPipelinePreprocessor preprocessor : pipesPreprocessors) {
			msg = preprocessor.beforeParsing(context.getJid(), msg);
			
			if (msg == null) {
				logger.info("Message has dropped by preprcessor before parsing. Session JID: '{}'. XMPP message: '{}'.",
						context.getJid(), originalMsg);
				
				return null;
			}
		}
		
		Object out = parseMessage(context, msg);
		
		if (out == null) {
			logger.warn("Null parsed object. Session JID: '{}'. XMPP message: '{}'.",
					context.getJid(), originalMsg);
			return null;
		}
		
		Object originalOut = out;
		for (IPipelinePreprocessor preprocessor : pipesPreprocessors) {
			out = preprocessor.afterParsing(context.getJid(), out);
			
			if (out == null) {
				logger.info("Message object has dropped by preprcessor after parsing. Session JID: '{}'. XMPP message: '{}'. Out object type: '{}'.",
						new Object[] {context.getJid(), originalMsg, originalOut.getClass().getName()});
				
				return null;
			}
		}
		
		return out;
	}

	private Object parseMessage(IConnectionContext context, String message) {
		if (logger.isTraceEnabled())
			logger.trace("Parsing message: {}.", message);
		
		Object out = null;
		try {
			out = parsingFactory.parse(message);
			if (out instanceof Stanza) {
				Stanza stanza = (Stanza)out;
				
				if (stanza.getFrom() == null) {
					stanza.setFrom(context.getJid());
				}
				
				// (rfc3920 9.1.2)
				if (!isValidFrom(context, stanza)) {
					throw new ProtocolException(new InvalidFrom());
				}
				
				if (logger.isTraceEnabled())
					logger.trace("Stanza parsed. Session JID: {}. XMPP message: '{}'. Parsed stanza: '{}'.",
							new Object[] {context.getJid(), message, stanza});
				
				// If server doesn't understand the extended namespaces(rfc3921 2.4)
				if (FlawedProtocolObject.isFlawed(stanza)) {
					if (logger.isTraceEnabled())
						logger.trace("Flawed stanza parsed. Session JID: '{}'. XMPP message: '{}'.",
								context.getJid(), message);
					
					out = processFlawedProtocolObject(stanza);
				}
				
				return out;
			}
			
			return out;
		} catch (ProtocolException e) {
			IError error = e.getError();
			if ((error instanceof StanzaError)) {
				amendStanzaError(out, (StanzaError)error);
			}
			
			if (logger.isTraceEnabled())
				logger.trace("Parsing protocol exception. Session JID: '{}'. XMPP message: '{}'.",
						context.getJid(), message);
			
			return error;
		} catch (RuntimeException e) {
			StanzaError error = new InternalServerError(CommonUtils.getInternalServerErrorMessage(e));
			amendStanzaError(out, error);
			
			logger.error(String.format("Parsing internal server error. Session JID: '{}'. XMPP message: '{}'.",
					context.getJid(), message), e);
			
			return error;
		}
	}

	private Object processFlawedProtocolObject(Stanza stanza) {
		if (isServerRecipient(stanza)) {
			if (stanza instanceof Iq) {
				Iq iq = (Iq)stanza;
				
				//If an entity receives an IQ stanza of type "get" or "set" containing a child element
				// qualified by a namespace it does not understand, the entity SHOULD return an
				// IQ stanza of type "error" with an error condition of <service-unavailable/>.
				if (iq.getType() == Iq.Type.SET || iq.getType() == Iq.Type.GET) {
					throw new ProtocolException(new ServiceUnavailable(String.format(
							"Flawed protocol object: %s.", stanza.getObject().toString())));
				}
			} else if (stanza instanceof Message) {
				// If an entity receives a message stanza whose only child element is qualified by a
				// namespace it does not understand, it MUST ignore the entire stanza.
				Message messageStanza = (Message)stanza;
				if (messageStanza.getSubjects().size() == 0 &&
						messageStanza.getBodies().size() == 0 &&
						messageStanza.getObjects().size() == 0 &&
						messageStanza.getThread() == null) {
					// Ignore the entire stanza
					return null;
				}
			} else {
				return stanza;
			}
		}
		
		return stanza;
	}

	private void amendStanzaError(Object parsed, StanzaError error) {
		if (!stanzaErrorAttachSenderMessage) {
			// Remove sender message
			error.setOriginalMessage(null);
		}
		
		if (parsed != null && parsed instanceof Stanza) {
			Stanza stanza = (Stanza)parsed;
			if (stanza.getId() != null) {
				error.setId(stanza.getId());
			}
		}
	}
	
	private boolean isValidFrom(IConnectionContext context, Stanza stanza) {
		return stanza.getFrom().getBareId().equals(context.getJid().getBareId());
	}

	private boolean isServerRecipient(Stanza stanza) {
		return stanza.getTo() == null || serverConfiguration.getDomainName().equals(stanza.getTo().toString());
	}
	
	@Override
	public void setConfiguration(IConfiguration configuration) {
		stanzaErrorAttachSenderMessage = configuration.getBoolean(CONFIGURATION_KEY_STANZA_ERROR_ATTACH_SENDER_MESSAGE, false);
	}

	@Override
	public void setServerConfiguration(IServerConfiguration serverConfiguration) {
		this.serverConfiguration = serverConfiguration;
	}

	@Override
	public void setApplicationComponentService(IApplicationComponentService appComponentService) {
		this.appComponentService = appComponentService;
	}
	
	private boolean isHeartbeats(String message) {
		for (char c : message.toCharArray()) {
			if (!(CHAR_HEART_BEAT == c))
				return false;
		}
		
		return true;
	}
}
