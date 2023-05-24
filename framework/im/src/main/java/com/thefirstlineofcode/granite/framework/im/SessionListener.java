package com.thefirstlineofcode.granite.framework.im;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.oxm.IOxmFactory;
import com.thefirstlineofcode.basalt.oxm.OxmService;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Stream;
import com.thefirstlineofcode.basalt.xmpp.core.stream.error.Conflict;
import com.thefirstlineofcode.granite.framework.core.annotations.BeanDependency;
import com.thefirstlineofcode.granite.framework.core.connection.IClientConnectionContext;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionManager;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionManagerAware;
import com.thefirstlineofcode.granite.framework.core.session.ISessionListener;

public class SessionListener implements ISessionListener, IConnectionManagerAware {
	private static final Logger logger = LoggerFactory.getLogger(SessionListener.class);
	
	@BeanDependency
	private IResourcesRegister resourcesRegister;
	
	@BeanDependency
	private IResourcesService resourceService;
	
	private IConnectionManager connectionManager;
	
	private static final IOxmFactory oxmFactory = OxmService.createMinimumOxmFactory();
	
	private static final String MESSAGE_CONFLICT = oxmFactory.translate(new Conflict());
	private static final String MESSAGE_CLOSE_STREAM = oxmFactory.translate(new Stream(true));

	@Override
	public void sessionEstablished(IConnectionContext context, JabberId sessionJid) throws Exception {
		try {
			resourcesRegister.register(sessionJid);
		} catch (ResourceRegistrationException e) {
			logger.error("Can't register resource. JID is {}.", sessionJid);
			throw e;
		}
		
	}

	@Override
	public void sessionClosing(IConnectionContext context, JabberId sessionJid) throws Exception {}
	
	@Override
	public void sessionClosed(IConnectionContext context, JabberId sessionJid) throws Exception {
		resourcesRegister.unregister(sessionJid);
	}

	@Override
	public void sessionEstablishing(IConnectionContext context, JabberId sessionJid) throws Exception {
		if (resourceService.getResource(sessionJid) != null) {
			IClientConnectionContext clientContext = (IClientConnectionContext)connectionManager.
					getConnectionContext(sessionJid);
			
			if (clientContext != null) {
				clientContext.write(MESSAGE_CONFLICT);
				clientContext.write(MESSAGE_CLOSE_STREAM);
				
				clientContext.close(true);
			}
		}
	}

	@Override
	public void setConnectionManager(IConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

}
