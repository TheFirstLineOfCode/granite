package com.thefirstlineofcode.granite.lite.pipeline;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.config.IConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.AbstractMessageReceiver;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageConnector;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEvent;
import com.thefirstlineofcode.granite.framework.core.repository.IComponentIdAware;
import com.thefirstlineofcode.granite.framework.core.session.ISession;
import com.thefirstlineofcode.granite.lite.pipeline.AbstractConnectionContext.MessageOutConnectionContext;
import com.thefirstlineofcode.granite.lite.pipeline.AbstractConnectionContext.ObjectOutConnectionContext;
import com.thefirstlineofcode.granite.lite.pipeline.AbstractConnectionContext.ProcessingContext;
import com.thefirstlineofcode.granite.lite.pipeline.AbstractConnectionContext.StringOutConnectionContext;

@Component(value="lite.stream.2.parsing.message.receiver",
	aliases={
		"lite.parsing.2.processing.message.receiver",
		"lite.any.2.event.message.receiver",
		"lite.any.2.routing.message.receiver"
	}
)
public class MessageReceiver extends AbstractMessageReceiver implements IMessageConnector, IConfigurationAware,
		IComponentIdAware, IServerConfigurationAware {
	private static final Logger logger = LoggerFactory.getLogger(MessageReceiver.class);
	
	private static final String CONFIGURATION_KEY_MESSAGE_QUEUE_MAX_SIZE = "message.queue.max.size";
	private static final int DEFAULT_MESSAGE_QUEUE_MAX_SIZE = 1024 * 64;
	
	private ArrayBlockingQueue<IMessage> messageQueue;
	private int messageQueueMaxSize;
	
	private ExecutorService executorService;
	private Thread messageReadingThread;
	
	protected String componentId;
	
	private volatile boolean stop;
	private String domain;
	
	@Override
	protected void doStart() throws Exception {
		stop = false;
		messageQueue = new ArrayBlockingQueue<>(messageQueueMaxSize);
		executorService = Executors.newCachedThreadPool();
		messageReadingThread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (!stop) {
					try {
						IMessage message = messageQueue.poll(50, TimeUnit.MILLISECONDS);
						if (message != null) {
							executorService.execute(getTask(message));
						}
					} catch (InterruptedException e) {
						if (logger.isTraceEnabled()) {
							logger.trace("Error[message receiver]. Connector ID: {}.", componentId, e);
						}
					}
				}
			}
			
		}, String.format("Granite Lite Message Receiver[%s]", componentId));
		messageReadingThread.start();
	}
	
	protected Runnable getTask(IMessage message) {
		return new WorkingThread(message);
	}
	
	private class WorkingThread implements Runnable {
		private IMessage message;
		
		public WorkingThread(IMessage message) {
			this.message = message;
		}
		
		public void run() {
			JabberId jid = (JabberId)(message.getHeaders().get(IMessage.KEY_SESSION_JID));
			
			if (!(message.getPayload() instanceof IEvent)) {
				if (jid == null) {
					logger.error("Null session ID. Connector ID: {}. Message: {}.", componentId, message.getPayload());
					return;
				}
			}
			
			IConnectionContext context = getConnectionContext(jid);
			if (context != null) {
				messageProcessor.process(context, message);
			} else {
				logger.error("Can't get connection context. Connector ID: {}. JID: {}.", componentId, jid);
			}
		}
	}

	@Override
	protected void doStop() throws Exception {
		stop = true;
		messageReadingThread.join();
		messageQueue = null;
		executorService = null;
	}

	@Override
	public void setConfiguration(IConfiguration configuration) {
		messageQueueMaxSize = configuration.getInteger(CONFIGURATION_KEY_MESSAGE_QUEUE_MAX_SIZE, DEFAULT_MESSAGE_QUEUE_MAX_SIZE);
	}
	
	@Override
	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}
	

	@Override
	public void put(IMessage message) {
		try {
			messageQueue.put(message);
		} catch (InterruptedException e) {
			logger.trace("Error[message receiver].", e);
		}
	}
	
	@Override
	public IConnectionContext getConnectionContext(JabberId sessionJid) {
		ISession session = null;
		
		if (sessionJid != null) {
			session = sessionManager.get(sessionJid);
			if (session == null && !domain.equals(sessionJid.toString()))
				return null;
		}
		
		if (Constants.CONNECTOR_ID_LITE_STREAM_2_PARSING.equals(componentId)) {
			return new ObjectOutConnectionContext(session, messageChannel);
		} else if (Constants.CONNECTOR_ID_LITE_PARSING_2_PROCESSING.equals(componentId)) {
			return new ProcessingContext(session, messageChannel);
		} else if (Constants.CONNECTOR_ID_LITE_ANY_2_ROUTING.equals(componentId)) {
			return new MessageOutConnectionContext(session, messageChannel);
		} else if (Constants.CONNECTOR_ID_LITE_ANY_2_EVENT.equals(componentId)) {
			return new MessageOutConnectionContext(session, messageChannel);
		} else if (Constants.CONNECTOR_ID_LITE_ROUTING_2_STREAM.equals(componentId)) {
			return new StringOutConnectionContext(session, messageChannel);
		}
		
		throw new RuntimeException(String.format("Unknown connector ID: %s.", componentId));
	}

	@Override
	public void setServerConfiguration(IServerConfiguration serverConfiguration) {
		domain = serverConfiguration.getDomainName();
	}

}
