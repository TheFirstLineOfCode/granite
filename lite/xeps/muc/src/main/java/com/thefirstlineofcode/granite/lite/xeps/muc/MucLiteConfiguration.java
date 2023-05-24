package com.thefirstlineofcode.granite.lite.xeps.muc;

import org.pf4j.Extension;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.thefirstlineofcode.granite.framework.adf.core.ISpringConfiguration;

@Extension
@Configuration
@ComponentScan
public class MucLiteConfiguration implements ISpringConfiguration {}
