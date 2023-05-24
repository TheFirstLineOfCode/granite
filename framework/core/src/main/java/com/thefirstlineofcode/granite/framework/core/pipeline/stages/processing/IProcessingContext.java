package com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;

public interface IProcessingContext extends IConnectionContext {
	void write(JabberId target, Object message);
}
