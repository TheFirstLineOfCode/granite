package com.thefirstlineofcode.granite.framework.core.adf.injection;

import java.lang.annotation.Annotation;

public interface IInjectionProvider {
	Class<? extends Annotation> getAnnotationType();
	Object getMark(Object source, Object dependencyAnnotation);
	IDependencyFetcher getFetcher(Object mark);
}
