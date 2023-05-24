package com.thefirstlineofcode.granite.pipeline.stages.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentServiceAware;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageProcessor;
import com.thefirstlineofcode.granite.framework.core.pipeline.SimpleMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.IPipelineExtendersContributor;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEvent;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventListener;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventListenerFactory;
import com.thefirstlineofcode.granite.framework.core.repository.IInitializable;
import com.thefirstlineofcode.granite.framework.core.utils.CommonUtils;

@Component("default.event.processor")
public class DefaultEventProcessor implements IMessageProcessor, IInitializable,
		IServerConfigurationAware, IApplicationComponentServiceAware {
	private static final String PACKAGE__PREFIX_OF_GRANITE = "com.thefirstlineofcode.granite";

	private Logger logger = LoggerFactory.getLogger(DefaultEventProcessor.class);
	
	protected Map<Class<?>, List<IEventListener<?>>> eventToListeners;
	
	private IApplicationComponentService appComponentService;
	private JabberId serverJid;
	
	public DefaultEventProcessor() {
		eventToListeners = new HashMap<>();
	}

	@Override
	public void process(IConnectionContext context, IMessage message) {
		try {
			IEvent event = (IEvent)message.getPayload();
			if (logger.isDebugEnabled())
				logger.debug("Begin to process the event. Event object: {}.", event);
			
			processEvent(context, event);
			
			if (logger.isDebugEnabled())
				logger.debug("End of processing the event. Event object: {}.", event);
		} catch (Exception e) {
			logger.error("Event processing error.", e);
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private <E extends IEvent> void processEvent(IConnectionContext context, E event) {
		List<IEventListener<?>> listeners = eventToListeners.get(event.getClass());
		if (listeners == null || listeners.size() == 0) {
			if (event.getClass().getName().startsWith(PACKAGE__PREFIX_OF_GRANITE))
				return;
			
			if (logger.isWarnEnabled()) {
				logger.warn("No event listener is listening to event which's type is {}.", event.getClass().getName());
			}
			
			return;
		}
		
		for (IEventListener<?> listener : listeners) {
			((IEventListener<E>)listener).process(getEventContext(context), (E)(event.clone()));
			
			if (logger.isTraceEnabled()) {
				logger.trace("The event has processed by event listener. Event object: {}. Event listener type: {}.",
						new Object[] {context.getJid(), event, listener.getClass().getName()});
			}
		}
	}

	private IEventContext getEventContext(IConnectionContext context) {
		return new EventContext(context);
	}
	
	private class EventContext implements IEventContext {
		private IConnectionContext connectionContext;
		
		public EventContext(IConnectionContext connectionContext) {
			this.connectionContext = connectionContext;
		}
		
		@Override
		public void write(Stanza stanza) {
			Map<Object, Object> headers = new HashMap<>();
			headers.put(IMessage.KEY_SESSION_JID, serverJid);
			
			connectionContext.write(new SimpleMessage(headers, stanza));
		}

		@Override
		public void write(JabberId target, Stanza stanza) {
			Map<Object, Object> headers = new HashMap<>();
			headers.put(IMessage.KEY_SESSION_JID, serverJid);
			headers.put(IMessage.KEY_MESSAGE_TARGET, target);
			
			connectionContext.write(new SimpleMessage(headers, stanza));
		}

		@Override
		public void write(JabberId target, String message) {
			Map<Object, Object> headers = new HashMap<>();
			headers.put(IMessage.KEY_SESSION_JID, serverJid);
			headers.put(IMessage.KEY_MESSAGE_TARGET, target);
			
			connectionContext.write(new SimpleMessage(headers, message));
		}
	}

	@Override
	public void init() {
		loadContributedEventListeners();
	}
	
	private void loadContributedEventListeners() {
		IPipelineExtendersContributor[] extendersContributors = CommonUtils.getExtendersContributors(appComponentService);
		
		for (IPipelineExtendersContributor extendersContributor : extendersContributors) {
			IEventListenerFactory<?>[] listenerFactories = extendersContributor.getEventListenerFactories();
			if (listenerFactories == null || listenerFactories.length == 0)
				continue;
			
			for (IEventListenerFactory<?> listenerFactory : listenerFactories) {
				List<IEventListener<?>> listeners = eventToListeners.get(listenerFactory.getType());
				if (listeners == null) {
					listeners = new ArrayList<>();
				}
				listeners.add(appComponentService.inject(listenerFactory.createListener()));
				
				eventToListeners.put(listenerFactory.getType(), listeners);
				if (logger.isDebugEnabled()) {
					logger.debug("Plugin '{}' contributed a event listener to listen '{}' event: '{}'.",
						new Object[] {
									appComponentService.getPluginManager().whichPlugin(extendersContributor.getClass()),
								listenerFactory.getType().getClass().getName(),
								listenerFactory.getClass().getName()
						}
					);
				}
			}
		}
	}
	
	@Override
	public void setServerConfiguration(IServerConfiguration serverConfiguration) {
		serverJid = JabberId.parse(serverConfiguration.getDomainName());
	}

	@Override
	public void setApplicationComponentService(IApplicationComponentService appComponentService) {
		this.appComponentService = appComponentService;
	}
}
