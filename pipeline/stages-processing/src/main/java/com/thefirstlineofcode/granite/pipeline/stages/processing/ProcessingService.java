package com.thefirstlineofcode.granite.pipeline.stages.processing;

import com.thefirstlineofcode.granite.framework.core.IService;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageReceiver;

@Component("processing.service")
public class ProcessingService implements IService {
	
	@Dependency("processing.message.receiver")
	private IMessageReceiver processingMessageReceiver;
	
	@Override
	public void start() throws Exception {
		processingMessageReceiver.start();
	}

	@Override
	public void stop() throws Exception {
		processingMessageReceiver.stop();
	}

}
