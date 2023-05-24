package com.thefirstlineofcode.granite.cluster.dba;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.FactoryBean;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.utils.IoUtils;

public class DatabaseFactoryBean implements FactoryBean<MongoDatabase>, IServerConfigurationAware {
	private volatile MongoClient client;
	private String dbName;
	
	private String configurationDir;
	
	@Override
	public MongoDatabase getObject() {
		if (client != null) {
			return client.getDatabase(dbName);
		}
		
		synchronized (this) {
			if (client != null)
				return client.getDatabase(dbName);
			
			File dbConfigFile = new File(configurationDir, "/db.ini");
			Properties properties = new Properties();
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(dbConfigFile));
				properties.load(reader);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(String.format("DB configuration file %s not found.", dbConfigFile), e);
			} catch (IOException e) {
				throw new RuntimeException(String.format("Can't read DB configuration file %s.", dbConfigFile), e);
			} finally {
				IoUtils.closeIO(reader);
			}
			
			List<ServerAddress> serverAddresses = getServerAddresses(properties.getProperty("addresses"));
			String dbName = properties.getProperty("db.name");
			String userName = properties.getProperty("user.name");
			String password = properties.getProperty("password");
			
			if (serverAddresses == null || serverAddresses.isEmpty()) {
				throw new RuntimeException("Invalid DB configuration. DB addresses is null.");
			}
			
			if (dbName == null) {
				throw new RuntimeException("Invalid DB configuration. DB name is null.");
			}
			
			if (userName == null) {
				throw new RuntimeException("Invalid DB configuration. User name is null.");
			}
			
			if (password == null) {
				throw new RuntimeException("Invalid DB configuration. Password is null.");
			}
			
			if (serverAddresses.size() == 1) {
				String connectionString = String.format("mongodb://%s:%s@%s/%s",
						userName, password, serverAddresses.get(0), dbName);
				client = MongoClients.create(connectionString);
			} else {
				MongoCredential credential = MongoCredential.createCredential(userName, dbName, password.toCharArray());
				MongoClientSettings settings = MongoClientSettings.builder().
						credential(credential).
						applyToClusterSettings(builder -> builder.hosts(serverAddresses)).
						build();
				client = MongoClients.create(settings);
			}
			
			this.dbName = dbName;
			MongoDatabase database = client.getDatabase(dbName);
			
			return database;
		}
	}

	private List<ServerAddress> getServerAddresses(String sAddresses) {
		StringTokenizer st = new StringTokenizer(sAddresses, ",");
		List<ServerAddress> serverAddresses = new ArrayList<>();
		
		while (st.hasMoreTokens()) {
			String sServerAddress = st.nextToken();
			int colonIndex = sServerAddress.indexOf(':');
			if (colonIndex == -1) {
				throw new RuntimeException(String.format("Invalid DB addresses: %s", sAddresses));
			}
			
			String host = sServerAddress.substring(0, colonIndex).trim();
			int port;
			try {
				port = Integer.parseInt(sServerAddress.substring(colonIndex + 1, sServerAddress.length()));
			} catch (NumberFormatException e) {
				throw new RuntimeException(String.format("Invalid DB addresses: %s", sAddresses), e);
			}
			
			serverAddresses.add(new ServerAddress(host, port));
		}
		
		return serverAddresses;
	}

	@Override
	public Class<?> getObjectType() {
		return MongoDatabase.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	@PreDestroy
	public void destroyClient() {
		if (client != null) {
			client.close();
		}
	}

	@Override
	public void setServerConfiguration(IServerConfiguration serverConfiguration) {
		configurationDir = serverConfiguration.getConfigurationDir();
	}
	
}
