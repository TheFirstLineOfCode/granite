package com.thefirstlineofcode.granite.framework.adf.core.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.springframework.context.ApplicationContext;

import com.thefirstlineofcode.granite.framework.core.adf.injection.IDependencyFetcher;
import com.thefirstlineofcode.granite.framework.core.adf.injection.IInjectionProvider;
import com.thefirstlineofcode.granite.framework.core.annotations.BeanDependency;

public class SpringBeanInjectionProvider implements IInjectionProvider {
	private ApplicationContext appContext;
	
	public SpringBeanInjectionProvider(ApplicationContext appContext) {
		this.appContext = appContext;
	}
	
	@Override
	public Class<? extends Annotation> getAnnotationType() {
		return BeanDependency.class;
	}

	@Override
	public IDependencyFetcher getFetcher(Object mark) {
		return new SpringBeanFetcher(appContext, getBeanType((Object[])mark), getQualifier((Object[])mark));
	}

	private Class<?> getBeanType(Object[] mark) {
		return (Class<?>)mark[0];
	}
	
	private String getQualifier(Object[] mark) {
		return (String)mark[1];
	}

	@Override
	public Object getMark(Object source, Object dependencyAnnotation) {
		Class<?> type = null;
		if (source instanceof Field) {
			Field field = (Field)source;
			type = field.getType();
		} else {
			Method method = (Method)source;
			type = method.getParameters()[0].getType();
		}
		
		String qualifier = null;
		BeanDependency beanDependency = (BeanDependency)dependencyAnnotation;
		qualifier = beanDependency.value();
		
		return new Object[] {type, qualifier};
	}
}
