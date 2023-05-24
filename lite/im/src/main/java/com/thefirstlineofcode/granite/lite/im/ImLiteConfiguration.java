package com.thefirstlineofcode.granite.lite.im;

import org.pf4j.Extension;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.thefirstlineofcode.granite.framework.adf.core.ISpringConfiguration;

@Extension
@Configuration
@ComponentScan
public class ImLiteConfiguration implements ISpringConfiguration {}
