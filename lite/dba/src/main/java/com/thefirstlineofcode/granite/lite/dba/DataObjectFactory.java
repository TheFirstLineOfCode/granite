package com.thefirstlineofcode.granite.lite.dba;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.thefirstlineofcode.granite.framework.adf.mybatis.DataObjectMapping;
import com.thefirstlineofcode.granite.framework.adf.mybatis.IDataContributor;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentServiceAware;
import com.thefirstlineofcode.granite.framework.core.adf.data.IDataObjectFactory;
import com.thefirstlineofcode.granite.framework.core.adf.data.IIdProvider;
import com.thefirstlineofcode.granite.framework.core.annotations.AppComponent;
import com.thefirstlineofcode.granite.framework.core.repository.IInitializable;

@AppComponent("data.object.factory")
public class DataObjectFactory implements IDataObjectFactory, IInitializable, IApplicationComponentServiceAware {	
	private IApplicationComponentService appComponentService;
	private Map<Class<?>, Class<?>> dataObjectMappings;
	private volatile boolean inited;
	
	public DataObjectFactory() {
		inited = false;
		dataObjectMappings = new HashMap<>();
	}
	
	@Override
	public void init() {
		if (inited)
			return;
		
		synchronized (this) {
			if (inited)
				return;
			
			List<IDataContributor> dataObjectsContributors = appComponentService.getPluginManager().
					getExtensions(IDataContributor.class);
			for (IDataContributor dataObjectsContributor : dataObjectsContributors) {
				DataObjectMapping<?>[] mappings = dataObjectsContributor.getDataObjectMappings();
				if (mappings == null || mappings.length == 0)
					continue;
				
				for (DataObjectMapping<?> mapping : mappings) {
					Class<?> domainType = mapping.domainType;
					if (domainType == null)
						domainType = mapping.dataType.getSuperclass();
					
					if (dataObjectMappings.containsKey(domainType))
						throw new IllegalArgumentException(String.format("Reduplicate domain data object type: '%s'.", mapping.domainType));
					
					dataObjectMappings.put(domainType, mapping.dataType);
				}
			}
			
			inited = true;
		}
	}
		
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public <K, V extends K> V create(Class<K> clazz) {
		if (!inited)
			init();
		
		try {
			V object = doCreate(clazz);
			
			if (object instanceof IIdProvider) {
				((IIdProvider)object).setId(UUID.randomUUID().toString());
			}
			
			return object;
		} catch (Exception e) {
			throw new RuntimeException(String.format("Can't create data object for class '%s'.", clazz.getName()), e);
		}
	}

	@SuppressWarnings("unchecked")
	private <K, V extends K> V doCreate(Class<K> domainType) throws InstantiationException, IllegalAccessException,
				IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Class<?> dataType = dataObjectMappings.get(domainType);
		if (dataType == null) {
			return (V)domainType.getDeclaredConstructor().newInstance();
		}
		
		return (V)dataType.getDeclaredConstructor().newInstance();
	}

	@Override
	public void setApplicationComponentService(IApplicationComponentService appComponentService) {
		this.appComponentService = appComponentService;
	}

}
