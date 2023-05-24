package com.thefirstlineofcode.granite.lite.dba;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.hsqldb.Server;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.ServerAcl.AclFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfigurationAware;

public class HSqlServer implements IServerConfigurationAware {
	private static final int DEFAULT_SERVER_PORT = 9001;
	private static final String DIRECTORY_NAME_DATA = "data";
	
	private static final Logger logger = LoggerFactory.getLogger(HSqlServer.class);
		
	private String serverHome;
	private String dataDir;
	private int port;
	
	private Server server;
	
	public HSqlServer() {
		this(DEFAULT_SERVER_PORT);
	}
	
	public HSqlServer(int port) {
		this.port = port;
	}
	
	@PostConstruct
    public void start() {
            try {
				doStart(dataDir);
				
				logger.info("HSQLDB has started.");
			} catch (Exception e) {
				logger.error("Can't start HSQLDB server.", e);
				throw new RuntimeException("Can't start HSQLDB server.", e);
			}
    }

	private void doStart(String dataDir) throws IOException, AclFormatException {
		server = new Server();
		HsqlProperties properties = new HsqlProperties();
		properties.setProperty("server.database.0", String.format("file:%s/%s", dataDir, "granite"));
		properties.setProperty("server.dbname.0", "granite");
		
		properties.setProperty("server.port", port);
		
		server.setLogWriter(null);
		server.setProperties(properties);

		server.start();
	}
	
	@PreDestroy
    public void stop() {
		try {
			if (server != null) {
				server.shutdownCatalogs(1);
				server = null;
			}
		} catch (Exception e) {
			logger.error("Can't stop HSQLDB server correctly.", e);
		}
            
		logger.info("HSQLDB has stopped.");
    }

	@Override
	public void setServerConfiguration(IServerConfiguration serverConfiguration) {
		serverHome = serverConfiguration.getServerHome();
		dataDir = String.format("%s/%s", serverHome, DIRECTORY_NAME_DATA);
		port = serverConfiguration.getHSqlPort();
	}
}