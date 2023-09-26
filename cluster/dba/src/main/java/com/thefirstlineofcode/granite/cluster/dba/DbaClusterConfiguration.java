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
import java.util.concurrent.TimeUnit;

import org.pf4j.Extension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ReadConcern;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.thefirstlineofcode.granite.framework.adf.core.ISpringConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.utils.IoUtils;

@Extension
@Configuration
public class DbaClusterConfiguration implements ISpringConfiguration, IServerConfigurationAware {
	private String configurationDir;
	private volatile MongoClient client;
	private String dbName;
	
	@Bean
	public MongoClient mongoClient() {
		if (client != null) {
			return client;
		}
		
		synchronized (this) {
			if (client != null)
				return client;
			
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
			
			this.dbName = dbName;
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
			
			return client;
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
	
	@Bean
	public MongoDatabase database(MongoClient mongoClient) {
		return mongoClient.getDatabase(dbName).
				withReadConcern(ReadConcern.MAJORITY).
				withWriteConcern(WriteConcern.MAJORITY.withWTimeout(1000, TimeUnit.MILLISECONDS));
	}
	
	@Bean
	public MongoDatabaseFactory mongoDbFactory() {
		return new SimpleMongoClientDatabaseFactory(mongoClient(), dbName);
	}
	
	@Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
	
	@Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, dbName);
    }
	
	@Bean(initMethod = "execute")
	public DbInitializationExecutor dbInitialzationExecutor(MongoDatabase database) {
		return new DbInitializationExecutor(database);
	}

	@Override
	public void setServerConfiguration(IServerConfiguration serverConfiguration) {
		configurationDir = serverConfiguration.getConfigurationDir();
	}
}
