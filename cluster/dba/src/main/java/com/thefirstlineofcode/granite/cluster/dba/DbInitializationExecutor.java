package com.thefirstlineofcode.granite.cluster.dba;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.mongodb.client.MongoDatabase;
import com.thefirstlineofcode.granite.framework.adf.mongodb.IDbInitializer;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentServiceAware;

@Component
public class DbInitializationExecutor implements IApplicationComponentServiceAware {
	private IApplicationComponentService appComponentService;
	private MongoDatabase database;
	
	public DbInitializationExecutor(MongoDatabase database) {
		this.database = database;
	}
	
	@PostConstruct
	public void execute() {
		List<Class<? extends IDbInitializer>> dbInitializerClasses = appComponentService.getExtensionClasses(IDbInitializer.class);
		if (dbInitializerClasses == null || dbInitializerClasses.size() == 0)
			return;
		
		for (Class<? extends IDbInitializer> dbInitializerClass : dbInitializerClasses) {
			IDbInitializer dbInitializer = appComponentService.createRawExtension(dbInitializerClass);
			dbInitializer.initialize(database);
		}
	}

	@Override
	public void setApplicationComponentService(IApplicationComponentService appComponentService) {
		this.appComponentService = appComponentService;
	}
}
