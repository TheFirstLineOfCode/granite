package com.thefirstlineofcode.granite.lite.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionManager;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.stream.IDeliveryMessageReceiver;

@Component(value="lite.routing.2.stream.message.receiver")
public class Routing2StreamMessageReceiver extends MessageReceiver implements IDeliveryMessageReceiver {
	private static final Logger logger = LoggerFactory.getLogger(Routing2StreamMessageReceiver.class);
	
	private IConnectionManager connectionManager;
	
	@Override
	protected void doStart() throws Exception {
		super.doStart();
	}

	@Override
	public void setConnectionManager(IConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}
	
	@Override
	public IConnectionContext getConnectionContext(JabberId sessionJid) {
		return connectionManager.getConnectionContext(sessionJid);
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
			JabberId jid = (JabberId)(message.getHeaders().get(IMessage.KEY_MESSAGE_TARGET));
			
			if (jid == null) {
				logger.warn("Null message target. Connector ID: {}. Message: {}.", componentId, message.getPayload());
				return;
			}
			
			IConnectionContext context = getConnectionContext(jid);
			if (context != null) {
				messageProcessor.process(context, message);
			} else {
				// TODO process offline message
			}
		}
	}

}
