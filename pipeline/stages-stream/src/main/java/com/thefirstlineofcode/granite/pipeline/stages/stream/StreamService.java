package com.thefirstlineofcode.granite.pipeline.stages.stream;

import java.util.List;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.IService;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionManager;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.stream.IClientMessageReceiver;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.stream.IDeliveryMessageReceiver;

@Component("stream.service")
public class StreamService implements IService {
	
	@Dependency("client.message.receivers")
	private List<IClientMessageReceiver> clientMessageReceivers;
	
	@Dependency("delivery.message.receiver")
	private IDeliveryMessageReceiver deliveryMessageReceiver;
	
	@Override
	public void start() throws Exception {
		if (deliveryMessageReceiver != null) {
			IConnectionManager connectionManager = getClientConnectionManager();
			
			if (connectionManager != null)
				deliveryMessageReceiver.setConnectionManager(connectionManager);
			
			deliveryMessageReceiver.start();
		}
		
		if (clientMessageReceivers != null) {
			for (IClientMessageReceiver clientMessageReceiver : clientMessageReceivers) {
				clientMessageReceiver.start();
			}
		}
	}

	private IConnectionManager getClientConnectionManager() {
		return new CompositeConnectionManager(clientMessageReceivers);
	}
	
	private class CompositeConnectionManager implements IConnectionManager {
		private List<IClientMessageReceiver> connectionManagers;
		
		public CompositeConnectionManager(List<IClientMessageReceiver> connectionManagers) {
			this.connectionManagers = connectionManagers;
		}

		@Override
		public IConnectionContext getConnectionContext(JabberId sessionJid) {
			if (connectionManagers == null)
				return null;
			
			for (IConnectionManager connectionManager : connectionManagers) {
				IConnectionContext context = connectionManager.getConnectionContext(sessionJid);
				
				if (context != null)
					return context;
			}
			
			return null;
		}
	}

	@Override
	public void stop() throws Exception {
		if (clientMessageReceivers != null) {
			for (IClientMessageReceiver clientMessageReceiver : clientMessageReceivers) {
				clientMessageReceiver.stop();
			}
			
			clientMessageReceivers.clear();
			clientMessageReceivers = null;
		}
		
		if (deliveryMessageReceiver != null) {
			deliveryMessageReceiver.stop();
			deliveryMessageReceiver = null;
		}
	}
	
}
