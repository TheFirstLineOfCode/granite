package com.thefirstlineofcode.granite.framework.core.log;

import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;

public class LogFilter extends TurboFilter {
	private final static String GRANITE_LIBRARIES_NAMESPACE = "com.thefirstlineofcode.granite.";
	private final static String SAND_LIBRARIES_NAMESPACE = "com.thefirstlineofcode.sand.";
	
	private boolean enableThirdpartyLogs = false;
	private String[] applicationNamespaces;
	
	public LogFilter(String[] applicationNamespaces, boolean enableThirdpartyLogs) {
		this.applicationNamespaces = applicationNamespaces;
		this.enableThirdpartyLogs = enableThirdpartyLogs;
	}
	
	@Override
	public FilterReply decide(Marker marker, Logger logger, Level level,
			String format, Object[] params, Throwable t) {
		if (enableThirdpartyLogs)
			return FilterReply.NEUTRAL;
		
		if (logger.getName().startsWith(GRANITE_LIBRARIES_NAMESPACE) ||
				logger.getName().startsWith(SAND_LIBRARIES_NAMESPACE)) {
			return FilterReply.NEUTRAL;
		}
		
		if (applicationNamespaces != null && applicationNamespaces.length != 0) {
			for (String applicationNamespace : applicationNamespaces) {
				if (logger.getName().startsWith(applicationNamespace))
					return FilterReply.NEUTRAL;
			}
		}
		
		return FilterReply.DENY;
	}

}
