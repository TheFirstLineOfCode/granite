package com.thefirstlineofcode.granite.pipeline.stages.processing;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.oxm.parsing.FlawedProtocolObject;
import com.thefirstlineofcode.basalt.xmpp.core.IError;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.Protocol;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.BadRequest;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.FeatureNotImplemented;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.InternalServerError;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.NotAllowed;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.ServiceUnavailable;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.StanzaError;
import com.thefirstlineofcode.basalt.xmpp.core.stream.error.StreamError;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentServiceAware;
import com.thefirstlineofcode.granite.framework.core.annotations.BeanDependency;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.auth.IAuthenticator;
import com.thefirstlineofcode.granite.framework.core.config.IConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.IPipelineExtendersContributor;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.DefaultIqResultProcessor;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IIqResultProcessor;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IXepProcessor;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IXepProcessorFactory;
import com.thefirstlineofcode.granite.framework.core.repository.IInitializable;
import com.thefirstlineofcode.granite.framework.core.session.ValueWrapper;
import com.thefirstlineofcode.granite.framework.core.utils.CommonUtils;
import com.thefirstlineofcode.granite.framework.im.DefaultSimpleMessageProcessor;
import com.thefirstlineofcode.granite.framework.im.DefaultSimplePresenceProcessor;
import com.thefirstlineofcode.granite.framework.im.IMessageProcessor;
import com.thefirstlineofcode.granite.framework.im.IPresenceProcessor;
import com.thefirstlineofcode.granite.framework.im.ISimpleStanzaProcessorsFactory;

@Component("default.protocol.processing.processor")
public class DefaultProtocolProcessingProcessor implements com.thefirstlineofcode.granite.framework.core.pipeline.IMessageProcessor,
		IConfigurationAware, IInitializable, IServerConfigurationAware, IApplicationComponentServiceAware {
	private static final Logger logger = LoggerFactory.getLogger(DefaultProtocolProcessingProcessor.class);
	
	private static final String CONFIGURATION_KEY_STANZA_ERROR_ATTACH_SENDER_MESSAGE = "stanza.error.attach.sender.message";
	private static final String CONFIGURATION_KEY_RELAY_UNKNOWN_NAMESPACE_IQ = "relay.unknown.namespace.iq";
	
	protected DefaultSimplePresenceProcessor defaultSimplePresenceProcessor;
	protected DefaultSimpleMessageProcessor defaultSimpleMessageProcessor;
	protected DefaultIqResultProcessor defaultIqResultProcessor;
	
	protected Map<ProtocolChain, IXepProcessor<?, ?>> singletonXepProcessors;
	protected Map<ProtocolChain, IXepProcessorFactory<?, ?>> xepProcessorFactories;
	
	private boolean stanzaErrorAttachSenderMessage;
	private boolean relayUnknownNamespaceIq;
	
	private IApplicationComponentService appComponentService;
	
	@BeanDependency("authenticator")
	private IAuthenticator authenticator;
	
	@BeanDependency("thingAuthenticator")
	private IAuthenticator thingAuthenticator;
	
	private JabberId domain;
	private JabberId[] domainAliases;
	
	public DefaultProtocolProcessingProcessor() {
		xepProcessorFactories = new HashMap<>();
		singletonXepProcessors = new HashMap<>();
	}

	@Override
	public synchronized void init() {
		loadSimpleStanzaProcessors();
		loadIqResultProcessors();
		loadContributedXepProcessors();
	}

	private void loadIqResultProcessors() {
		defaultIqResultProcessor = new DefaultIqResultProcessor();
		appComponentService.inject(defaultIqResultProcessor);
		
		IPipelineExtendersContributor[] extendersContributors = CommonUtils.getExtendersContributors(appComponentService);
		
		for (IPipelineExtendersContributor extendersContributor : extendersContributors) {
			IIqResultProcessor[] iqResultProcessors = extendersContributor.getIqResultProcessors();
			if (iqResultProcessors == null || iqResultProcessors.length == 0)
				continue;
			
			for (IIqResultProcessor iqResultProcessor : iqResultProcessors) {
				appComponentService.inject(iqResultProcessor);
				defaultIqResultProcessor.addIqResultProcessor(iqResultProcessor);
				
				if (logger.isDebugEnabled()) {
					logger.debug("Plugin '{}' contributed a IQ result processor: '{}'.",
							appComponentService.getPluginManager().whichPlugin(extendersContributor.getClass()),
							iqResultProcessor.getClass().getName()
					);
				}
			}
		}
		
	}

	private void loadSimpleStanzaProcessors() {
		defaultSimpleMessageProcessor = new DefaultSimpleMessageProcessor();
		appComponentService.inject(defaultSimpleMessageProcessor);
		
		defaultSimplePresenceProcessor = new DefaultSimplePresenceProcessor();
		appComponentService.inject(defaultSimplePresenceProcessor);
		
		List<Class<? extends ISimpleStanzaProcessorsFactory>> stanzaProcessorFactoryClasses = appComponentService.getExtensionClasses(ISimpleStanzaProcessorsFactory.class);
		if (stanzaProcessorFactoryClasses == null || stanzaProcessorFactoryClasses.size() == 0) {
			logger.info("Simple stanza factorory not be found. XMPP IM protocol may not be supported");
			return;
		}
		
		for (Class<? extends ISimpleStanzaProcessorsFactory> stanzaProcessorFactoryClass : stanzaProcessorFactoryClasses) {
			ISimpleStanzaProcessorsFactory stanzaProcessorsFactory = appComponentService.createExtension(stanzaProcessorFactoryClass);
			logger.info("Simple stanza factory {} has been found.", stanzaProcessorsFactory.getClass().getName());
			
			for (IMessageProcessor messageProcessor : stanzaProcessorsFactory.getMessageProcessors()) {
				appComponentService.inject(messageProcessor);
				defaultSimpleMessageProcessor.addMessageProcessor(messageProcessor);
			}
			
			for (IPresenceProcessor presenceProcessor : stanzaProcessorsFactory.getPresenceProcessors()) {
				appComponentService.inject(presenceProcessor);
				defaultSimplePresenceProcessor.addPresenceProcessor(presenceProcessor);
			}
		}
	}
	
	protected void loadContributedXepProcessors() {
		IPipelineExtendersContributor[] extendersContributors = CommonUtils.getExtendersContributors(appComponentService);
		
		for (IPipelineExtendersContributor extendersContributor : extendersContributors) {
			
			IXepProcessorFactory<?, ?>[] processorFactories = extendersContributor.getXepProcessorFactories();
			if (processorFactories == null || processorFactories.length == 0)
				continue;
			
			for (IXepProcessorFactory<?, ?> processorFactory : processorFactories) {
				if (processorFactory.isSingleton()) {
					IXepProcessor<?, ?> xepProcessor;
					try {
						xepProcessor = processorFactory.createProcessor();
						xepProcessor = appComponentService.inject(xepProcessor);
					} catch (Exception e) {
						logger.error(String.format("Can't create singleton XEP processor by factory: '%s'.",
								processorFactory.getClass().getName()), e);
						
						throw new RuntimeException(String.format("Can't create singleton XEP processor by factory: '%s'",
								processorFactory.getClass().getName()), e);
					}
					
					singletonXepProcessors.put(processorFactory.getProtocolChain(), xepProcessor);
				} else {
					xepProcessorFactories.put(processorFactory.getProtocolChain(), processorFactory);
				}
				
				if (logger.isDebugEnabled()) {
					logger.debug("Plugin '{}' contributed a protocol processor factory: '{}'.",
							appComponentService.getPluginManager().whichPlugin(extendersContributor.getClass()),
							processorFactory.getClass().getName()
					);
				}
			}
		}
	}

	@Override
	public void process(IConnectionContext context, IMessage message) {
		try {
			if (logger.isDebugEnabled())
				logger.debug("Begin to process the protocol object. Session JID: {}. Protocol object: {}.",
						context.getJid(), message.getPayload());
			
			doProcess((IProcessingContext)context, message);
			
			if (logger.isDebugEnabled())
				logger.debug("End of processing the protocol object. Session JID: {}. Protocol object: {}.",
						context.getJid(), message.getPayload());
		} catch (ProtocolException e) {
			processProtocolException(context, e, message.getPayload());
		} catch (RuntimeException e) {
			processRuntimeException(context, e, message.getPayload());
		} catch (ComplexStanzaProtocolException e) {
			for (Exception exception : e.getExceptions()) {
				if (exception instanceof ProtocolException) {
					processProtocolException(context, (ProtocolException)exception, message.getPayload());
				} else {
					// exception instanceof RuntimeException
					processRuntimeException(context, (RuntimeException)exception, message.getPayload());
				}
			}
		}
	}

	private void processRuntimeException(IConnectionContext context, RuntimeException e, Object message) {
		if (e instanceof UndeclaredThrowableException) {
			ProtocolException pe = findProtocolException(e);
			
			if(pe != null) {
				processProtocolException(context, pe, message);
				return;
			}
		}
		
		outputRuntimeExceptionError(context, e, message);
	}

	private ProtocolException findProtocolException(Throwable e) {
		Throwable current = e;
		
		while (current.getCause() != null) {
			if (current.getCause() instanceof ProtocolException) {
				return (ProtocolException)current.getCause();
			}
			
			current = current.getCause();
		}
		
		return null;
	}

	private void outputRuntimeExceptionError(IConnectionContext context, RuntimeException e, Object message) {
		if (message instanceof Stanza) {
			context.write(createStanzaError(context, e, message));
		} else {
			context.write(new com.thefirstlineofcode.basalt.xmpp.core.stream.error.InternalServerError(CommonUtils.getInternalServerErrorMessage(e)));
			context.close();
		}
		
		logger.error(String.format("Processing runtime exception. Session JID: %s. Protocol object: %s.",
				context.getJid(), message), e);
	}

	private StanzaError createStanzaError(IConnectionContext context, RuntimeException e, Object message) {
		Stanza stanza = (Stanza)message;
		StanzaError error = new com.thefirstlineofcode.basalt.xmpp.core.stanza.error.InternalServerError(CommonUtils.getInternalServerErrorMessage(e));
		
		error = amendStanzaError(context, error, stanza);
		
		return error;
	}

	private StanzaError amendStanzaError(IConnectionContext context, StanzaError error, Stanza stanza) {
		if (error.getKind() == null) {
			if (stanza instanceof Message) {
				error.setKind(StanzaError.Kind.MESSAGE);
			} else if (stanza instanceof Presence) {
				error.setKind(StanzaError.Kind.PRESENCE);
			} else {
				error.setKind(StanzaError.Kind.IQ);
			}
		}
		
		if (error.getId() == null) {
			error.setId(stanza.getId());
		}
		
		if (error.getTo() == null) {
			JabberId to = stanza.getFrom() == null ? context.getJid() : stanza.getFrom();
			error.setTo(to);
		}
		
		if (error.getFrom() == null) {
			error.setFrom(stanza.getTo() == null ? domain : stanza.getTo());
		}
		
		return error;
	}

	private void processProtocolException(IConnectionContext context, ProtocolException e, Object message) {
		IError error = e.getError();
		
		if ((StanzaError.class.isAssignableFrom(error.getClass())) && (message instanceof Stanza)) {
			error = amendStanzaError(context, (StanzaError)error, (Stanza)message);
		}
		
		context.write(e.getError());
		
		logger.error(String.format("Processing protocol exception. Session JID: %s. Protocol object: %s.",
				context.getJid(), message), e);
		
		if (error instanceof StreamError) {
			context.close();
		}
	}
	
	private class AttachOriginalMessageConnectionContextProxy implements IProcessingContext {
		private IProcessingContext original;
		private String originalMessage;
		
		public AttachOriginalMessageConnectionContextProxy(IProcessingContext original,
				String originalMessage) {
			this.original = original;
			this.originalMessage = originalMessage;
		}

		@Override
		public <T> T setAttribute(Object key, T value) {
			return original.setAttribute(key, value);
		}

		@Override
		public <T> T getAttribute(Object key) {
			return original.getAttribute(key);
		}

		@Override
		public <T> T getAttribute(Object key, T defaultValue) {
			return original.getAttribute(key, defaultValue);
		}

		@Override
		public <T> T removeAttribute(Object key) {
			return original.removeAttribute(key);
		}

		@Override
		public Object[] getAttributeKeys() {
			return original.getAttributeKeys();
		}

		@Override
		public JabberId getJid() {
			return original.getJid();
		}

		@Override
		public void write(Object message) {
			if (message instanceof StanzaError) {
				((StanzaError)message).setOriginalMessage(originalMessage);
			}
			
			original.write(message);
		}

		@Override
		public void close() {
			original.close();
		}

		@Override
		public void write(JabberId target, Object message) {
			if (message instanceof StanzaError) {
				((StanzaError)message).setOriginalMessage(originalMessage);
			}
			
			original.write(target, message);
		}

		@Override
		public <T> T setAttribute(Object key, ValueWrapper<T> wrapper) {
			return original.setAttribute(key, wrapper);
		}
		
	}

	private void doProcess(IProcessingContext context, IMessage message) throws RuntimeException,
				ComplexStanzaProtocolException {
		Object object = message.getPayload();
		
		if (stanzaErrorAttachSenderMessage && (object instanceof Stanza)) {
			String originalMessage = ((Stanza)object).getOriginalMessage();
			context = new AttachOriginalMessageConnectionContextProxy(context, originalMessage);
		}
		
		if (object instanceof Stanza) {
			Stanza stanza = (Stanza)object;
			
			if (logger.isTraceEnabled()) {
				logger.trace("Try to process stanza. Session JID: {}. Stanza object: {}. Original message: {}.",
						new Object[] {context.getJid(), stanza, stanza.getOriginalMessage()});
			}
			
			if (FlawedProtocolObject.isFlawed(stanza)) {
				logger.error("Flawed project object. Ignore to process it. Session JID: {}. Protocol object: {}.", context.getJid(), stanza);
				throw new ProtocolException(new BadRequest(String.format("Ignore to process flawed project object: %s.", stanza)));
			}
			
			// Try to process stanza in a more simply way if it's a simple structure stanza. 
			if (processSimpleStanza(context, stanza)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Simple stanza. It's processed by simple stanza processor. Session JID: {}. Stanza object: {}.",
							context.getJid(), stanza);
				}
				
				return;
			}
			
			if (stanza.getObjects().size() == 1) {				
				processXep(context, stanza, stanza.getObject());
			} else {				
				processComplexStanza(context, stanza);
			}
		} else {
			context.write(object);
		}
	}
	
	private void processComplexStanza(IProcessingContext context, Stanza stanza)
			throws ComplexStanzaProtocolException {
		boolean processed = false;
		List<Exception> exceptions = new ArrayList<>();
		for (Object object : stanza.getObjects()) {
			if (object instanceof FlawedProtocolObject) {
				if (logger.isTraceEnabled())
					logger.trace("Protocol object is flawed protocol object. Ignore to process it. Session JID: {}. Stanza object: {}.",
							context.getJid(), stanza);
				
				continue;
			}
			
			try {
				processXep(context, stanza, object);
				processed = true;
			} catch (Exception e) {
				if (e instanceof ProtocolException) {
					ProtocolException pe = ((ProtocolException)e);
					// ServiceUnavailable error will be thrown if protocol is not a supported XEP.
					if (!(pe.getError() instanceof ServiceUnavailable)) {
						processed = true;
					}
				}
				exceptions.add(e);
			}
		}
		
		if (FlawedProtocolObject.isFlawed(stanza)) {
			FlawedProtocolObject flawed = stanza.getObject(FlawedProtocolObject.class);
			if (flawed != null) {
				for (ProtocolChain protocolChain : flawed.getFlaws()) {
					// We find a top level protocol object that is embedded into stanza instantly.
					if (protocolChain.size() == 2) {
						exceptions.add(new ProtocolException(new ServiceUnavailable(
								String.format("Unsupported protocol: %s.", protocolChain))));
					}
				}
			}
		}
		
		if (!processed) {
			processed = processStanza(context, stanza);
		}
		
		if (!processed) {
			throw new ComplexStanzaProtocolException(exceptions);
		}
	}
	
	private boolean processStanza(IProcessingContext context, Stanza stanza) {
		if (stanza instanceof Presence) {
			return processPresence(context, (Presence)stanza);
		} else if (stanza instanceof Message) {
			return processMessage(context, (Message)stanza);
		} else {
			return processIq(context, (Iq)stanza);
		}
	}

	private boolean processIq(IProcessingContext context, Iq iq) {
		if (!relayUnknownNamespaceIq)
			return false;
		
		if (!isServerReceipt(iq))
			return false;
		
		String userName = iq.getTo().getNode();
		if (userName == null)
			return false;
		
		if (!authenticator.exists(userName) && !thingAuthenticator.exists(userName)) {
			return false;
		}
		
		context.write(iq);
		return true;
	}

	private class ComplexStanzaProtocolException extends Exception {
		private static final long serialVersionUID = 946967617544711594L;
		
		private List<Exception> exceptions;
		
		public ComplexStanzaProtocolException(List<Exception> exceptions) {
			this.exceptions = exceptions;
		}
		
		public List<Exception> getExceptions() {
			return exceptions;
		}
	}

	private boolean processSimpleStanza(IProcessingContext context, Stanza stanza) {
		if (isXep(stanza) && !isIqResult(stanza))
			return false;
		
		if (stanza instanceof Presence) {
			if (!processPresence(context, (Presence)stanza)) {
				throw new ProtocolException(new ServiceUnavailable());
			}
			
			return true;
		} else if (stanza instanceof Message) {
			if (!processMessage(context, (Message)stanza)) {
				throw new ProtocolException(new ServiceUnavailable());
			}
			
			return true;
		} else if (stanza instanceof Iq) {
			processIqResult(context, (Iq)stanza);
			return true;
		} else if (stanza instanceof StanzaError &&
				((StanzaError)stanza).getKind() == StanzaError.Kind.IQ) {			
			// Stanza is an IQ error.
			processIqError(context, (StanzaError)stanza);
			return true;
		} else {
			return true;
		}
	}

	private boolean isXep(Stanza stanza) {
		return !stanza.getObjects().isEmpty();
	}
	
	private boolean isIqResult(Stanza stanza) {
		if (!(stanza instanceof Iq))
			return false;
		
		return Iq.Type.RESULT == ((Iq)stanza).getType();
	}

	private void processIqError(IProcessingContext context, StanzaError error) {
		if (error.getId() == null) {
			logger.error("Null stanza error ID. Session JID: {}. Stanza error object: {}.", context.getJid(), error);
			throw new ProtocolException(new BadRequest("Null stanza error ID."));
		}
		
		if (defaultIqResultProcessor == null) {
			throw new ProtocolException(new ServiceUnavailable());
		}
		
		defaultIqResultProcessor.processError(context, error);
	}
	
	private void processIqResult(IProcessingContext context, Iq iq) {
		if (iq.getType() != Iq.Type.RESULT) {
			logger.error("Neither XEP nor IQ result. Session JID: {}. IQ object: {}.", context.getJid(), iq);
			throw new ProtocolException(new BadRequest("Neither XEP nor IQ result."));
		}
		
		if (iq.getId() == null) {
			logger.error("Null IQ ID. Session JID: {}. IQ object: {}.", context.getJid(), iq);
			throw new ProtocolException(new BadRequest("Null ID."));
		}
		
		if (defaultIqResultProcessor == null) {
			throw new ProtocolException(new ServiceUnavailable());
		}
		
		defaultIqResultProcessor.processResult(context, iq);
	}
	
	private boolean processMessage(IProcessingContext context, Message message) {
		if (defaultSimpleMessageProcessor != null && defaultSimpleMessageProcessor.process(context, message)) {
			return true;
		}
		
		return false;
	}

	private boolean processPresence(IProcessingContext context, Presence presence) {
		if (defaultSimplePresenceProcessor != null) {
			return defaultSimplePresenceProcessor.process(context, presence);
		}
		
		return false;
	}
	
	private <K extends Stanza, V> void processXep(IProcessingContext context, Stanza stanza, Object xep) throws RuntimeException {
		if (isServerReceipt(stanza)) {
			doProcessXep(context, stanza, xep);
			return;
		}
		
		if (isToForeignDomain(stanza.getTo())) {
			deliverXepToForeignDomain(context, stanza);
		}
	}

	private boolean isServerReceipt(Stanza stanza) {
		return stanza.getTo() == null || isToDomain(stanza.getTo()) || isToDomainAlias(stanza.getTo());
	}

	private boolean isToDomain(JabberId to) {
		return to.getDomain().equals(domain.getDomain());
	}
	
	private boolean isToDomainAlias(JabberId to) {
		if (domainAliases.length == 0)
			return false;
		
		for (JabberId domainAlias : domainAliases) {
			if (domainAlias.getDomain().equals(to.getDomain()))
				return true;
		}
		
		return false;
	}

	private void deliverXepToForeignDomain(IProcessingContext context, Stanza stanza) {
		if (stanza.getFrom() != null && !stanza.getFrom().equals(context.getJid())) {
			logger.error("'from' attribute should be {}.", context.getJid());
			throw new ProtocolException(new NotAllowed(String.format("'from' attribute should be %s.", context.getJid())));
		}
		
		// TODO Server Rules for Handling XML Stanzas(rfc3920 10)
		throw new ProtocolException(new FeatureNotImplemented("Feature delivering XEP to foreign domain isn't implemented yet."));
	}

	private boolean isToForeignDomain(JabberId to) {
		if (to == null)
			return false;
		
		JabberId toDomain = new JabberId(to.getDomain());
		if (isToDomain(toDomain) || isToDomainAlias(toDomain))
			return false;
		
		return true;
	}

	@SuppressWarnings("unchecked")
	private <K extends Stanza, V> boolean doProcessXep(IProcessingContext context, Stanza stanza, Object xep) {
		if (logger.isDebugEnabled())
			logger.debug("Try to process stanza by XEP processor. Session JID: {}. Stanza object: {}.",
					context.getJid(), stanza);
		
		ProtocolChain protocolChain = getXepProtocolChain(stanza, xep);
		
		IXepProcessor<K, V> xepProcessor = getXepProcessor(protocolChain);
		if (xepProcessor == null) {
			Protocol protocol = stanza.getObjectProtocol(stanza.getObject().getClass());
			logger.error("Unsupported protocol: {}. Session JID: {}.", protocol, context.getJid());
			throw new ProtocolException(new ServiceUnavailable(String.format("Unsupported protocol: %s.",
					protocol)));
		}
		
		xepProcessor.process(context, (K)stanza, (V)xep);
		
		if (logger.isDebugEnabled())
			logger.debug("Stanza is processed by XEP processor. Session JID: {}. Stanza: {}. XEP processor: {}.",
					context.getJid(), stanza, xepProcessor);
		
		return true;
	}
	
	@SuppressWarnings("unchecked")
	private <K extends Stanza, V> IXepProcessor<K, V> getXepProcessor(ProtocolChain protocolChain) {
		IXepProcessor<K, V> xepProcessor = (IXepProcessor<K, V>)singletonXepProcessors.get(protocolChain);
		
		if (xepProcessor == null) {
			xepProcessor = createXepProcessorByFactory(protocolChain);
		}
		
		return xepProcessor;
	}

	@SuppressWarnings("unchecked")
	private <V, K extends Stanza> IXepProcessor<K, V> createXepProcessorByFactory(ProtocolChain protocolChain) {
		IXepProcessorFactory<?, ?> processorFactory = xepProcessorFactories.get(protocolChain);
		if (processorFactory == null) {
			logger.error("Unsupported protocol: {}.", protocolChain);
			throw new ProtocolException(new ServiceUnavailable(String.format("Unsupported protocol: %s.", protocolChain)));
		}
		
		try {
			IXepProcessor<K, V> xepProcessor = (IXepProcessor<K, V>)processorFactory.createProcessor();
			return appComponentService.inject(xepProcessor);
		} catch (Exception e) {
			logger.error(String.format("Can't instantiate XEP processor by factory: '%s'.",
					processorFactory.getClass().getName()), e);
			throw new ProtocolException(new InternalServerError(
					String.format("Can't instantiate XEP processor by factory: '%s'.",
					processorFactory.getClass().getName()), e));
		}
	}

	private ProtocolChain getXepProtocolChain(Stanza stanza, Object xep) {
		return ProtocolChain.first(getStanzaProtocol(stanza)).next(stanza.getObjectProtocol(xep.getClass()));
	}

	private Protocol getStanzaProtocol(Stanza stanza) {
		if (stanza instanceof Iq)
			return Iq.PROTOCOL;
		
		if (stanza instanceof Presence)
			return Presence.PROTOCOL;
		
		if (stanza instanceof Message)
			return Message.PROTOCOL;
		
		return null;
	}
	
	@Override
	public void setConfiguration(IConfiguration configuration) {
		stanzaErrorAttachSenderMessage = configuration.getBoolean(
			CONFIGURATION_KEY_STANZA_ERROR_ATTACH_SENDER_MESSAGE,
				false);
		relayUnknownNamespaceIq = configuration.getBoolean(
				CONFIGURATION_KEY_RELAY_UNKNOWN_NAMESPACE_IQ,
				false);
	}
	
	@Override
	public void setServerConfiguration(IServerConfiguration serverConfiguration) {
		domain = JabberId.parse(serverConfiguration.getDomainName());
		String[] sDomainAliasNames = serverConfiguration.getDomainAliasNames();
		if (sDomainAliasNames.length != 0) {
			domainAliases = new JabberId[sDomainAliasNames.length];
			
			for (int i = 0; i < sDomainAliasNames.length; i++) {
				domainAliases[i] = JabberId.parse(sDomainAliasNames[i]);
			}
		} else {
			domainAliases = new JabberId[0];
		}
	}
	
	@Override
	public void setApplicationComponentService(IApplicationComponentService appComponentService) {
		this.appComponentService = appComponentService;
	}
}
