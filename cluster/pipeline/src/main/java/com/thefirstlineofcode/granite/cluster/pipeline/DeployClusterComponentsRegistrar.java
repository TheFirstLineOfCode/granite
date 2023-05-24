package com.thefirstlineofcode.granite.cluster.pipeline;

import java.io.File;

import org.apache.ignite.Ignite;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlan;
import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlanException;
import com.thefirstlineofcode.granite.cluster.node.commons.deploying.DeployPlanReader;
import com.thefirstlineofcode.granite.framework.core.repository.CreationException;
import com.thefirstlineofcode.granite.framework.core.repository.GenericComponentInfo;
import com.thefirstlineofcode.granite.framework.core.repository.IRepository;
import com.thefirstlineofcode.granite.framework.core.repository.IRepositoryAware;
import com.thefirstlineofcode.granite.framework.core.repository.Repository;

public class DeployClusterComponentsRegistrar implements IRepositoryAware, ApplicationContextAware {
	private static final String PROPERTY_KEY_NODE_TYPE = "granite.node.type";
	private static final String PROPERTY_KEY_GRANITE_DEPLOY_PLAN_FILE = "granite.deploy.plan.file";
	
	private Repository repository;
	private ApplicationContext applicationContext;
	
	public void registerDeployClusterComponents() {
		Ignite ignite = applicationContext.getBean(Ignite.class);
		if (ignite == null)
			throw new RuntimeException("Null ignite instance.");
		
		repository.registerComponent(new GenericComponentInfo(Constants.COMPONENT_ID_CLUSTER_IGNITE, Ignite.class) {
			@Override
			public Object doCreate() throws CreationException {
				return ignite;
			}
		});
		
		DeployPlan deployPlan = readDeployPlan();
		repository.registerComponent(new GenericComponentInfo(Constants.COMPONENT_ID_CLUSTER_NODE_RUNTIME_CONFIGURATION,
				NodeRuntimeConfiguration.class) {
			@Override
			public Object doCreate() throws CreationException {
				return new NodeRuntimeConfiguration(System.getProperty(PROPERTY_KEY_NODE_TYPE), deployPlan);
			}
		});
	}
	
	private DeployPlan readDeployPlan() {
		String deployFilePath = System.getProperty(PROPERTY_KEY_GRANITE_DEPLOY_PLAN_FILE);
		
		try {
			return new DeployPlanReader().read(new File(deployFilePath).toPath());
		} catch (DeployPlanException e) {
			throw new RuntimeException("Can't read deploy configuration file.", e);
		}
	}
	
	@Override
	public void setRepository(IRepository repository) {
		this.repository = (Repository)repository;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
