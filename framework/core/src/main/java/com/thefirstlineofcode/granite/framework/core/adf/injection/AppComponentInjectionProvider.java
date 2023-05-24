package com.thefirstlineofcode.granite.framework.core.adf.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;

public class AppComponentInjectionProvider implements IInjectionProvider {
	private IApplicationComponentService appComponentService;
	
	public AppComponentInjectionProvider(IApplicationComponentService appComponentService) {
		this.appComponentService = appComponentService;
	}

	@Override
	public Class<? extends Annotation> getAnnotationType() {
		return Dependency.class;
	}

	@Override
	public IDependencyFetcher getFetcher(Object mark) {
		return new AppComponentFetcher(appComponentService, (IdAndType)mark);
	}

	@Override
	public Object getMark(Object source, Object dependencyAnnotation) {
		Dependency dependency = (Dependency)dependencyAnnotation;
		String componentId = dependency.value();
		
		Class<?> type;
		if (source instanceof Field) {
			type = ((Field)source).getType();
		} else {
			type = ((Method)source).getParameters()[0].getType();
		}
		
		return new IdAndType(componentId, type);
	}

}
