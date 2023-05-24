package com.thefirstlineofcode.granite.pipeline.stages.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.connection.IConnectionContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageProcessor;

@Component("default.delivery.message.processor")
public class DefaultDeliveryMessageProcessor implements IMessageProcessor {
	private Logger logger = LoggerFactory.getLogger(DefaultDeliveryMessageProcessor.class);

	@Override
	public void process(IConnectionContext context, IMessage message) {
		try {
			context.write(message.getPayload());
		} catch (Exception e) {
			logger.error("Routing error.", e);
		}
		
	}

}
