package com.thefirstlineofcode.granite.cluster.pipeline;

import org.pf4j.Extension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.thefirstlineofcode.granite.framework.adf.core.ISpringConfiguration;

@Extension
@Configuration
public class PipelineClusterConfiguration implements ISpringConfiguration {
	@Bean(destroyMethod = "destroy")
	public IgniteFactoryBean ignite() {
		return new IgniteFactoryBean();
	}
}
