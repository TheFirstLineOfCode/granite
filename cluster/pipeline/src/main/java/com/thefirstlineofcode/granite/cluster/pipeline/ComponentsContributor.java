package com.thefirstlineofcode.granite.cluster.pipeline;

import org.pf4j.Extension;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.thefirstlineofcode.granite.framework.core.repository.IComponentsContributor;
import com.thefirstlineofcode.granite.framework.core.repository.IComponentsRegisteredCallback;
import com.thefirstlineofcode.granite.framework.core.repository.IRepository;

@Extension
public class ComponentsContributor implements IComponentsContributor,
		IComponentsRegisteredCallback, ApplicationContextAware {	
	private ApplicationContext appContext;
	
	@Override
	public Class<?>[] getComponentClasses() {
		return new Class<?>[] {
			Any2EventMessageChannel.class,
			Any2EventMessageReceiver.class,
			Any2RoutingMessageChannel.class,
			Any2RoutingMessageReceiver.class,
			Parsing2ProcessingMessageChannel.class,
			Parsing2ProcessingMessageReceiver.class,
			Routing2StreamMessageChannel.class,
			Routing2StreamMessageReceiver.class,
			Stream2ParsingMessageChannel.class,
			Stream2ParsingMessageReceiver.class,
			LocalNodeIdProvider.class,
			Router.class
		};
	}

	@Override
	public void componentsRegistered(IRepository repository) {
		DeployClusterComponentsRegistrar registrar = new DeployClusterComponentsRegistrar();
		registrar.setApplicationContext(appContext);
		registrar.setRepository(repository);
		
		registrar.registerDeployClusterComponents();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.appContext = applicationContext;
	}

}
