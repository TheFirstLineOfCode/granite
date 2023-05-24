package com.thefirstlineofcode.granite.framework.adf.core;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.pf4j.PluginManager;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.thefirstlineofcode.granite.framework.adf.core.injection.SpringBeanInjectionProvider;
import com.thefirstlineofcode.granite.framework.core.adf.ApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.adf.CompositeClassLoader;
import com.thefirstlineofcode.granite.framework.core.adf.data.IDataObjectFactory;
import com.thefirstlineofcode.granite.framework.core.adf.data.IDataObjectFactoryAware;
import com.thefirstlineofcode.granite.framework.core.adf.injection.AppComponentInjectionProvider;
import com.thefirstlineofcode.granite.framework.core.adf.injection.IInjectionProvider;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;

public class AdfComponentService extends ApplicationComponentService {
	protected AnnotationConfigApplicationContext appContext;
	protected IDataObjectFactory dataObjectFactory;
	
	public AdfComponentService(IServerConfiguration serverConfiguration) {
		super(serverConfiguration);
	}
	
	protected ClassLoader[] registerSpringConfigurations() {		
		registerPredefinedSpringConfigurations();
		
		return registerContributedSpringConfigurations();
	}
	
	public ApplicationContext getApplicationContext() {
		return appContext;
	}

	private ClassLoader[] registerContributedSpringConfigurations() {
		List<Class<? extends ISpringConfiguration>> contributedSpringConfigurationClasses =
				pluginManager.getExtensionClasses(ISpringConfiguration.class);
		if (contributedSpringConfigurationClasses == null || contributedSpringConfigurationClasses.size() == 0)
			return null;
		
		List<ClassLoader> classLoaders = new ArrayList<>();
		for (Class<? extends ISpringConfiguration> contributedSpringConfigurationClass : contributedSpringConfigurationClasses) {			
			appContext.register(contributedSpringConfigurationClasses.toArray(
					new Class<?>[contributedSpringConfigurationClasses.size()]));
			
			classLoaders.add(contributedSpringConfigurationClass.getClassLoader());
		}
		
		return classLoaders.toArray(new ClassLoader[classLoaders.size()]);
	}
	
	protected void registerPredefinedSpringConfigurations() {}

	@Override
	protected PluginManager createPluginManager() {
		Path pluginsDirPath = Paths.get(serverConfiguration.getPluginsDir());
		AdfPluginManager pluginManager = new AdfPluginManager(pluginsDirPath);
		pluginManager.setApplicationComponentService(this);
		
		return pluginManager;
	}
	
	@Override
	public <T> T inject(T rawInstance) {
		return inject(rawInstance, true);
	}
	
	public <T> T inject(T rawInstance, boolean injectAppContext) {
		T injectedInstance = super.inject(rawInstance);
		
		if (injectAppContext) {
			injectedInstance = injectAppContext(injectAppContext, injectedInstance);
		}
		
		return injectedInstance;
	}

	private <T> T injectAppContext(boolean injectAppContext, T injectedInstance) {
		if (!injectAppContext)
			return injectedInstance;
		
		if (injectedInstance instanceof ApplicationContextAware) {
			((ApplicationContextAware)injectedInstance).setApplicationContext(appContext);
		}
		
		return injectedInstance;
	}
	
	@Override
	protected <T> void injectByAwareInterfaces(T instance) {
		super.injectByAwareInterfaces(instance);
		injectDataObjectFactory(instance);
	}
	
	private void injectDataObjectFactory(Object instance) {
		if (!(instance instanceof IDataObjectFactoryAware))
			return;
			
		if (dataObjectFactory == null) {
			synchronized (this) {
				if (dataObjectFactory == null)
					dataObjectFactory = (IDataObjectFactory)getAppComponent(
							IDataObjectFactory.COMPONENT_ID_DATA_OBJECT_FACTORY,
							IDataObjectFactory.class);
			}
		}
		
		if (dataObjectFactory == null)
			throw new RuntimeException("Can't find a data object factory to do application component injection.");
		
		((IDataObjectFactoryAware)instance).setDataObjectFactory(dataObjectFactory);
	}
	
	@Override
	protected IInjectionProvider[] getInjectionProviders() {
		return new IInjectionProvider[] {new AppComponentInjectionProvider(this), new SpringBeanInjectionProvider(appContext)};
	}
	
	@Override
	public void start() {
		super.start();
		
		appContext = new AnnotationConfigApplicationContext();
		
		ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory)appContext.getBeanFactory();
		beanFactory.addBeanPostProcessor(new AdfSpringBeanPostProcessor(this));
		
		ClassLoader[] classLoaders = registerSpringConfigurations();
		if (classLoaders != null) {
			appContext.setClassLoader(new CompositeClassLoader(classLoaders));
		}
		
		appContext.refresh();
	}
	
	@Override
	public void stop() {
		if (appContext != null)
			appContext.close();
		
		super.stop();
	}
}
