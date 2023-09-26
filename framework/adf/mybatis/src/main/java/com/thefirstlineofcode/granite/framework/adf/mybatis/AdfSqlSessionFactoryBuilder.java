package com.thefirstlineofcode.granite.framework.adf.mybatis;

import java.net.URL;
import java.util.List;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.type.EnumTypeHandler;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.Protocol;
import com.thefirstlineofcode.basalt.xmpp.datetime.DateTime;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentService;

public class AdfSqlSessionFactoryBuilder extends SqlSessionFactoryBuilder {
	private static final String PREFIX_NAME_DATA_OBJECT_TYPE_COC = "D_";
	
	private IApplicationComponentService appComponentService;
	
	public AdfSqlSessionFactoryBuilder(IApplicationComponentService appComponentService) {
		this.appComponentService = appComponentService;
	}

	@Override
	public SqlSessionFactory build(org.apache.ibatis.session.Configuration configuration) {
		loadPredefinedTypeHandlers(configuration);
		
		List<IDataContributor> dataContributors = appComponentService.getPluginManager().getExtensions(IDataContributor.class);
		if (dataContributors == null || dataContributors.size() == 0) {
			return super.build(configuration);			
		}
		
		loadContributedData(configuration, dataContributors);
		return super.build(configuration);
	}

	private void loadContributedData(org.apache.ibatis.session.Configuration configuration,
			List<IDataContributor> dataContributors) {
		ClassLoader defaultClassLoader = Resources.getDefaultClassLoader();
		for (IDataContributor dataContributor : dataContributors) {
			Resources.setDefaultClassLoader(dataContributor.getClass().getClassLoader());
			TypeHandlerMapping[] typeHandlerMaps = dataContributor.getTypeHandlerMappings();
			if (typeHandlerMaps != null && typeHandlerMaps.length != 0) {
				registerTypeHandlers(configuration, typeHandlerMaps);
			}
			
			DataObjectMapping<?>[] dataObjectMaps = dataContributor.getDataObjectMappings();
			if (dataObjectMaps != null && dataObjectMaps.length != 0) {
				registerDataObjectAliases(configuration, dataObjectMaps);
			}
			
			URL[] mappers = dataContributor.getMappers();
			if (mappers != null && mappers.length != 0) {
				registerMappers(configuration, mappers, dataContributor);
			}
		}
		Resources.setDefaultClassLoader(defaultClassLoader);
	}

	private void registerMappers(org.apache.ibatis.session.Configuration configuration, URL[] mappers, IDataContributor dataContributor) {
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(dataContributor.getClass().getClassLoader());
			for (URL mapper : mappers) {				
				try {
					XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(mapper.openStream(),
						configuration, mapper.toString(), configuration.getSqlFragments());
					xmlMapperBuilder.parse();
				} catch (Exception e) {
					// TODO: handle exception
					throw new RuntimeException(String.format("Failed to parse mapper resource: '%s'.", mapper.toString()), e);
				}
			}
		} finally {
			Thread.currentThread().setContextClassLoader(oldClassLoader);
		}
	}

	private void registerDataObjectAliases(org.apache.ibatis.session.Configuration configuration,
			DataObjectMapping<?>[] dataObjectMaps) {
		for (DataObjectMapping<?> dataObjectMap : dataObjectMaps) {
			if (dataObjectMap.domainType != null) {
				configuration.getTypeAliasRegistry().registerAlias(dataObjectMap.domainType.getSimpleName(),
						dataObjectMap.dataType);					
			} else {
				String name = dataObjectMap.dataType.getSimpleName();					
				if (name.startsWith(PREFIX_NAME_DATA_OBJECT_TYPE_COC)) {
					name = name.substring(2, name.length());
				}
				
				configuration.getTypeAliasRegistry().registerAlias(name, dataObjectMap.dataType);
			}
		}
	}

	private void registerTypeHandlers(org.apache.ibatis.session.Configuration configuration,
			TypeHandlerMapping[] typeHandlerMaps) {
		for (TypeHandlerMapping typeHandlerMap : typeHandlerMaps) {
			if (typeHandlerMap.type == null) {
				configuration.getTypeHandlerRegistry().register(typeHandlerMap.typeHandlerType);;
			} else {
				configuration.getTypeHandlerRegistry().register(typeHandlerMap.type, typeHandlerMap.typeHandlerType);
			}
			configuration.getTypeAliasRegistry().registerAlias(typeHandlerMap.typeHandlerType.getSimpleName(), typeHandlerMap.typeHandlerType);
		}
	}

	private void loadPredefinedTypeHandlers(org.apache.ibatis.session.Configuration configuration) {
		configuration.getTypeHandlerRegistry().register(JabberId.class, JabberIdTypeHandler.class);
		configuration.getTypeHandlerRegistry().register(Protocol.class, ProtocolTypeHandler.class);
		configuration.getTypeHandlerRegistry().register(DateTime.class, DateTimeTypeHandler.class);
		
		configuration.getTypeAliasRegistry().registerAlias("JabberId", JabberId.class);
		configuration.getTypeAliasRegistry().registerAlias("JabberIdTypeHandler", JabberIdTypeHandler.class);
		configuration.getTypeAliasRegistry().registerAlias("Protocol", Protocol.class);
		configuration.getTypeAliasRegistry().registerAlias("ProtocolTypeHandler", ProtocolTypeHandler.class);
		configuration.getTypeAliasRegistry().registerAlias("EnumTypeHandler", EnumTypeHandler.class);
		configuration.getTypeAliasRegistry().registerAlias("DateTimeTypeHandler", DateTimeTypeHandler.class);
	}
}
