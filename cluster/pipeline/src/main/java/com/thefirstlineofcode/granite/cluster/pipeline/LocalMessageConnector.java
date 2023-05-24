package com.thefirstlineofcode.granite.cluster.pipeline;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stream.error.NotAuthorized;
import com.thefirstlineofcode.granite.framework.core.config.IConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.AbstractMessageReceiver;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageConnector;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEvent;
import com.thefirstlineofcode.granite.framework.core.repository.IComponentIdAware;
import com.thefirstlineofcode.granite.framework.core.session.ISession;

public abstract class LocalMessageConnector extends AbstractMessageReceiver
		implements IMessageConnector, IConfigurationAware, IComponentIdAware {
	private static final Logger logger = LoggerFactory.getLogger(LocalMessageConnector.class);
	
	protected ArrayBlockingQueue<IMessage> messageQueue;
	protected int messageQueueMaxSize;
	
	protected ExecutorService executorService;
	protected Thread messageReaderThread;
	
	protected volatile boolean stop;
	
	protected String componentId;
	
	@Override
	protected void doStart() throws Exception {
		stop = false;
		messageQueue = new ArrayBlockingQueue<>(messageQueueMaxSize);
		executorService = Executors.newCachedThreadPool();
		messageReaderThread = new Thread(new Runnable() {

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
							logger.trace("Message reader thread interrupted. Message receiver: {}.", getClass().getName(), e);
						}
					}
				}
			}
			
		}, String.format("%s Message Reader Thread.", componentId));
		messageReaderThread.start();
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
					logger.warn("Null session id. Connector ID: {}. Message: {}.", componentId, message.getPayload());
					return;
				}
			}
			
			IConnectionContext context = getConnectionContext(jid);
			if (context != null) {
				messageProcessor.process(context, message);
			} else {
				logger.warn("Can't get connection context. Connector ID: {}. JID: {}.", componentId, jid);
			}
		}
	}

	@Override
	protected void doStop() throws Exception {
		stop = true;
		messageReaderThread.join();
		messageQueue = null;
		executorService = null;
	}

	@Override
	public void setConfiguration(IConfiguration configuration) {
		messageQueueMaxSize = configuration.getInteger(getMessageQueueMaxSizeConfigurationKey(), getDefaultMessageQueueMaxSize());
	}

	@Override
	public void put(IMessage message) {
		try {
			messageQueue.put(message);
		} catch (InterruptedException e) {
			logger.trace("error[message receiver]", e);
		}
	}
	
	@Override
	public IConnectionContext getConnectionContext(JabberId sessionJid) {
		if (sessionJid == null)
			throw new RuntimeException("Null session jid.");
		
		ISession session = sessionManager.get(sessionJid);
		if (session == null)
			throw new ProtocolException(new NotAuthorized(String.format("Null session. JID: %s.", sessionJid)));
		
		return doGetConnectionContext(session);
	}
	
	@Override
	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}
	
	protected abstract int getDefaultMessageQueueMaxSize();
	protected abstract String getMessageQueueMaxSizeConfigurationKey();
	protected abstract IConnectionContext doGetConnectionContext(ISession session);
}
