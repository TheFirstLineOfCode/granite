package com.thefirstlineofcode.granite.framework.core.pipeline.stages.routing;

import com.thefirstlineofcode.basalt.oxm.translating.ITranslator;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.IPipelineExtender;

public interface IProtocolTranslatorFactory<T> extends IPipelineExtender {
	Class<T> getType();
	ITranslator<T> createTranslator();
}
