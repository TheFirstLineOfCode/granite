package com.thefirstlineofcode.granite.framework.core.pipeline.stages.parsing;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.IPipelineExtender;

public interface IPipelinePreprocessor extends IPipelineExtender {
	String beforeParsing(JabberId from, String message);
	Object afterParsing(JabberId from, Object object);
}
