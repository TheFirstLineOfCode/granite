package com.thefirstlineofcode.granite.framework.adf.core;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class AdfBeanPostProcessor implements BeanPostProcessor {
	private AdfComponentService adfComponentService;
	
	public AdfBeanPostProcessor(AdfComponentService adfComponentService) {
		this.adfComponentService = adfComponentService;
	}
	
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return adfComponentService.inject(bean, false);
	}
}
