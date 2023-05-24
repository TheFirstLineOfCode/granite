package com.thefirstlineofcode.granite.framework.core.adf;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.oxm.parsing.IParser;
import com.thefirstlineofcode.basalt.oxm.translating.ITranslator;
import com.thefirstlineofcode.granite.framework.core.adf.injection.AppComponentInjectionProvider;
import com.thefirstlineofcode.granite.framework.core.adf.injection.FieldDependencyInjector;
import com.thefirstlineofcode.granite.framework.core.adf.injection.IDependencyFetcher;
import com.thefirstlineofcode.granite.framework.core.adf.injection.IDependencyInjector;
import com.thefirstlineofcode.granite.framework.core.adf.injection.IInjectionProvider;
import com.thefirstlineofcode.granite.framework.core.adf.injection.MethodDependencyInjector;
import com.thefirstlineofcode.granite.framework.core.annotations.AppComponent;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.config.IConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageChannel;
import com.thefirstlineofcode.granite.framework.core.pipeline.SimpleMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.IPipelineExtender;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEvent;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventFirer;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventFirerAware;
import com.thefirstlineofcode.granite.framework.core.repository.CreationException;
import com.thefirstlineofcode.granite.framework.core.repository.IComponentIdAware;
import com.thefirstlineofcode.granite.framework.core.repository.IComponentInfo;
import com.thefirstlineofcode.granite.framework.core.repository.IDependencyInfo;
import com.thefirstlineofcode.granite.framework.core.repository.IInitializable;
import com.thefirstlineofcode.granite.framework.core.repository.IRepository;
import com.thefirstlineofcode.granite.framework.core.repository.IRepositoryAware;
import com.thefirstlineofcode.granite.framework.core.utils.CommonUtils;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;

public class ApplicationComponentService implements IApplicationComponentService {
	private static final String COMPONENT_ID_LITE_ANY_2_EVENT_MESSAGE_CHANNEL = "lite.any.2.event.message.channel";
	private static final String COMPONENT_ID_CLUSTER_ANY_2_EVENT_MESSAGE_CHANNEL = "cluster.any.2.event.message.channel";
	
	private static final Logger logger = LoggerFactory.getLogger(ApplicationComponentService.class);
	
	protected IServerConfiguration serverConfiguration;
	protected PluginManager pluginManager;
	protected IPluginConfigurations pluginConfigurations;
	protected boolean syncPlugins;
	protected boolean started;
	protected IRepository repository;
	protected Map<String, IComponentInfo> appComponentInfos;
	protected Map<Class<?>, List<IDependencyInjector>> dependencyInjectors;

	public ApplicationComponentService(IServerConfiguration serverConfiguration) {
		this.serverConfiguration = serverConfiguration;
		
		appComponentInfos = new HashMap<>();
		dependencyInjectors = new HashMap<>();
	}
	
	protected PluginConfigurations readPluginConfigurations(IServerConfiguration serverConfiguration) {
		return new PluginConfigurations(serverConfiguration.getConfigurationDir());
	}
	
	protected PluginManager createPluginManager() {
		return new AppComponentPluginManager(this);
	}
	
	@Override
	public PluginManager getPluginManager() {
		return pluginManager;
	}
	
	@Override
	public IPluginConfigurations getPluginConfigurations() {
		return pluginConfigurations;
	}

	@Override
	public <T> List<Class<? extends T>> getExtensionClasses(Class<T> type) {
		return pluginManager.getExtensionClasses(type);
	}

	@Override
	public <T> T createExtension(Class<T> type) {
		T extension = createRawExtension(type);
		
		if (extension == null)
			return null;
		
		return inject(extension);
	}
	
	@Override
	public <T> T createRawExtension(Class<T> type) {
		T extension = null;
		try {
			extension = type.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(String.format("Can't create raw extension which's type is '%s'.", type.getName()), e);
		}
		
		return extension;
	}

	@Override
	public void start() {
		if (started)
			return;
		
		pluginConfigurations = readPluginConfigurations(serverConfiguration);
		initPlugins();
		
		loadContributedAppComponents();
		
		started = true;
	}
	
	protected void loadContributedAppComponents() {
		List<IAppComponentsContributor> componentContributors = pluginManager.getExtensions(IAppComponentsContributor.class);
		if (componentContributors == null || componentContributors.size() == 0)
			return;
		
		for (IAppComponentsContributor componentContributor : componentContributors) {
			Class<?>[] appComponentClasses = componentContributor.getAppComponentClasses();
			if (appComponentClasses == null || appComponentClasses.length == 0)
				continue;
			
			for (Class<?> appComponentClass : appComponentClasses) {
				AppComponent appComponentAnnotation = appComponentClass.getAnnotation(AppComponent.class);
				if (appComponentAnnotation == null) {
					throw new IllegalArgumentException(
							String.format("Class '%s' isn't a legal application component. " +
									"You need to add @AppComponent to the class.", appComponentClass.getName()));
				}
				
				registerAppComponentInfo(appComponentAnnotation, appComponentClass);
			}
		}
	}

	protected void registerAppComponentInfo(AppComponent appComponentAnnotation, Class<?> appComponentClass) {
		String appComponentId = appComponentAnnotation.value();
		IComponentInfo componentInfo = new AppComponentInfo(appComponentId, appComponentClass,
				appComponentAnnotation.isSingleton());
		if (appComponentInfos.containsKey(appComponentId)) {
			throw new RuntimeException(String.format("Reduplicate application component ID: %s.", appComponentId));
		}
		
		appComponentInfos.put(appComponentId, componentInfo);
	}
	
	private class AppComponentInfo implements IComponentInfo {
		private String id;
		private Class<?> type;
		private boolean singleton;
		private volatile Object instance;
		private Object singletonLock = new Object();
		
		public AppComponentInfo(String id, Class<?> type, boolean singleton) {
			this.id = id;
			this.type = type;
			this.singleton = singleton;
		}
		
		@Override
		public String getId() {
			return id;
		}
		
		@Override
		public void addDependency(IDependencyInfo dependency) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void removeDependency(IDependencyInfo dependency) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public IDependencyInfo[] getDependencies() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public boolean isAvailable() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public boolean isService() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public Object create() throws CreationException {
			if (!singleton) {
				try {
					return doCreate();
				} catch (Exception e) {
					throw new CreationException(String.format("Can't create application component %s.", id), e);
				}
			}
			
			synchronized (singletonLock) {
				if (instance == null) {
					try {
						instance = doCreate();
					} catch (Exception e) {
						throw new CreationException(String.format("Can't create application component %s.", id), e);
					}
				}
				
				return instance;
			}
		}

		private Object doCreate() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
					InvocationTargetException, NoSuchMethodException, SecurityException {
			Object component = type.getDeclaredConstructor().newInstance();
			inject(component);
			
			return component;
		}
		
		@Override
		public boolean isSingleton() {
			return singleton;
		}
		
		@Override
		public IComponentInfo getAliasComponent(String alias) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public String toString() {
			return String.format("Application component['%s', '%s'].", id, type.getName());
		}

		@Override
		public Class<?> getType() {
			return type;
		}
	}
	
	@Override
	public boolean isStarted() {
		return started;
	}

	protected void initPlugins() {
		pluginManager = createPluginManager();
		
		pluginManager.loadPlugins();
		pluginManager.startPlugins();
	}

	@Override
	public void stop() {
		if (!started)
			return;
		
		try {			
			destroyPlugins();
		} catch (Exception e) {
			logger.warn("Something was wrong when we stopped the plugins.", e);
		}
		
		started = false;
	}

	private void destroyPlugins() {
		pluginManager.stopPlugins();
		pluginManager.unloadPlugins();
	}

	@Override
	public <T> T inject(T rawInstance) {
		Class<?> clazz = rawInstance.getClass();
		
		for (IDependencyInjector injector : getDependencyInjectors(clazz)) {
			injector.inject(rawInstance);
		}
		
		injectByAwareInterfaces(rawInstance);
		
		if (rawInstance instanceof IInitializable) {
			((IInitializable)rawInstance).init();
		}
		
		return rawInstance;
	}

	protected <T> void injectByAwareInterfaces(T rawInstance) {
		if (rawInstance instanceof IServerConfigurationAware) {
			((IServerConfigurationAware)rawInstance).setServerConfiguration(serverConfiguration);
		}
		
		if (rawInstance instanceof IConfigurationAware) {
			Class<?> type = rawInstance.getClass();
			PluginWrapper plugin = pluginManager.whichPlugin(type);
			if (plugin == null)
				throw new IllegalArgumentException(
					String.format("Can't determine which plugin the extension which's class name is '%s' is load from.", type));
			
			IConfiguration configuration = pluginConfigurations.getConfiguration(plugin.getDescriptor().getPluginId());
			((IConfigurationAware)rawInstance).setConfiguration(configuration);
		}
		
		if (rawInstance instanceof IApplicationComponentServiceAware) {
			((IApplicationComponentServiceAware)rawInstance).setApplicationComponentService(this);
		}
		
		if (rawInstance instanceof IEventFirerAware) {
			((IEventFirerAware)rawInstance).setEventFirer(createEventFirer());
		}
	}
	
	protected List<IDependencyInjector> getDependencyInjectors(Class<?> clazz) {
		List<IDependencyInjector> injectors = dependencyInjectors.get(clazz);
		
		if (injectors != null)
			return injectors;
		
		IInjectionProvider[] injectionProviders = getInjectionProviders();
		
		injectors = new ArrayList<>();
		scanInjectors(clazz, injectors, injectionProviders);	
		
		List<IDependencyInjector> old = dependencyInjectors.putIfAbsent(clazz, injectors);
		return old == null ? injectors : old;
	}

	private void scanInjectors(Class<?> clazz, List<IDependencyInjector> injectors,
			IInjectionProvider[] injectionProviders) {
		scanFieldInjectors(clazz, injectionProviders, injectors);
		scanMethodInjectors(clazz, injectionProviders, injectors);
		
		Class<?> parentClass = clazz.getSuperclass();
		if (parentClass != Object.class) {
			scanFieldInjectors(parentClass, injectionProviders, injectors);
			scanMethodInjectors(parentClass, injectionProviders, injectors);
		}
	}

	private void scanMethodInjectors(Class<?> clazz, IInjectionProvider[] injectionProviders,
			List<IDependencyInjector> injectors) {
		for (Method method : clazz.getMethods()) {
			for (IInjectionProvider injectionProvider : injectionProviders) {
				if (method.getDeclaringClass().equals(Object.class))
					continue;
				
				int modifiers = method.getModifiers();
				if (!Modifier.isPublic(modifiers) ||
						Modifier.isAbstract(modifiers) ||
						Modifier.isStatic(modifiers))
					continue;
				
				Object dependencyAnnotation = method.getAnnotation(injectionProvider.getAnnotationType());				
				if (dependencyAnnotation != null) {
					if (!CommonUtils.isSetterMethod(method))
						logger.warn("Dependency method '{}' isn't a setter method.", method);
					
					Object mark = injectionProvider.getMark(method, dependencyAnnotation);
					IDependencyFetcher fetcher = injectionProvider.getFetcher(mark);
					IDependencyInjector injector = new MethodDependencyInjector(method, fetcher);
					injectors.add(injector);
				}
			}
		}
	}

	private void scanFieldInjectors(Class<?> clazz, IInjectionProvider[] injectionProviders,
			List<IDependencyInjector> injectors) {
		for (Field field : getClassFields(clazz, null)) {
			for (IInjectionProvider injectionProvider : injectionProviders) {
				Object dependencyAnnotation = field.getAnnotation(injectionProvider.getAnnotationType());				
				if (dependencyAnnotation != null) {
					Object mark = injectionProvider.getMark(field, dependencyAnnotation);
					IDependencyFetcher fetcher = injectionProvider.getFetcher(mark);
					IDependencyInjector injector = new FieldDependencyInjector(field, fetcher);
					injectors.add(injector);
				}
			}
		}
	}
	
	protected IInjectionProvider[] getInjectionProviders() {
		return new IInjectionProvider[] {new AppComponentInjectionProvider(this)};
	}
	
	private List<Field> getClassFields(Class<?> clazz, List<Field> fields) {
		if (fields == null)
			fields = new ArrayList<Field>();
		
		for (Field field : clazz.getDeclaredFields()) {
			fields.add(field);
		}
		
		Class<?> parent = clazz.getSuperclass();
		if (!isPipelineExtender(parent) && parent.getAnnotation(Component.class) == null)
			return fields;
		
		return getClassFields(parent, fields);
	}
	
	private boolean isPipelineExtender(Class<?> clazz) {
		return IParser.class.isAssignableFrom(clazz) ||
			ITranslator.class.isAssignableFrom(clazz) ||
			IPipelineExtender.class.isAssignableFrom(clazz);
	}
	
	@Override
	public void setRepository(IRepository repository) {
		this.repository = repository;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAppComponent(String id, Class<T> type) {
		Enhancer enhancer = new Enhancer();
		
		enhancer.setSuperclass(type);
		enhancer.setCallback(new LazyLoadComponentInvocationHandler(id));
		enhancer.setUseFactory(false);
		enhancer.setClassLoader(getClassLoader(type));
		
		return (T)enhancer.create();
	}

	private <T> ClassLoader getClassLoader(Class<T> type) {
		PluginWrapper plugin = pluginManager.whichPlugin(type);
		if (plugin != null) {
			return plugin.getPluginClassLoader();
		}
		
		return type.getClassLoader();
	}
	
	private class LazyLoadComponentInvocationHandler implements InvocationHandler {
		private String id;
		private volatile Object component = null;
		
		public LazyLoadComponentInvocationHandler(String id) {
			this.id = id;
		}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			component = getComponent();
			
			if (component == null)
				throw new IllegalStateException(String.format("Null dependency '%s'.", id));
			
			if (Proxy.isProxyClass(component.getClass())) {
				return Proxy.getInvocationHandler(component).invoke(component, method, args);
			} else if (net.sf.cglib.proxy.Proxy.isProxyClass(component.getClass())) {
				return net.sf.cglib.proxy.Proxy.getInvocationHandler(component).invoke(component, method, args);				
			} else {
				return method.invoke(component, args);				
			}
		}

		private Object getComponent() throws CreationException {
			if (component != null)
				return component;
			
			synchronized (this) {
				if (component != null)
					return component;
				
				IComponentInfo componentInfo = appComponentInfos.get(id);
				if (componentInfo == null)
					return null;
				
				component = componentInfo.create();
			}
			
			return component;
		}
	}
	
	@Override
	public IEventFirer createEventFirer() {
		return new EventFirer(repository);
	}
	
	private class EventFirer implements IEventFirer {
		private IRepository repository;
		private IMessageChannel messageChannel;
		
		public EventFirer(IRepository repository) {
			this.repository = repository;
		}
		
		@Override
		public void fire(IEvent event) {
			if (messageChannel == null) {
				messageChannel = getAnyToEventMessageChannel();
			}
			
			messageChannel.send(new SimpleMessage(event));
		}
		
		private IMessageChannel getAnyToEventMessageChannel() {
			String componentId = COMPONENT_ID_CLUSTER_ANY_2_EVENT_MESSAGE_CHANNEL;
			IMessageChannel messageChannel = (IMessageChannel)repository.get(componentId);
			if (messageChannel == null) {
				componentId = COMPONENT_ID_LITE_ANY_2_EVENT_MESSAGE_CHANNEL;
				messageChannel = (IMessageChannel)repository.get(componentId);
			}
			
			if (messageChannel == null)
				throw new RuntimeException("Can't fire event because the any to event message channel is null.");
			
			if (messageChannel instanceof IComponentIdAware) {
				((IComponentIdAware) messageChannel).setComponentId(componentId);
			}
			
			if (messageChannel instanceof IRepositoryAware) {
				((IRepositoryAware)messageChannel).setRepository(repository);
			}
			
			return messageChannel;
		}
	}
}
