package com.thefirstlineofcode.granite.framework.im;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.framework.core.utils.OrderComparator;

public class DefaultSimpleMessageProcessor implements IMessageProcessor {
	private List<IMessageProcessor> messageProcessors;	
	
	public DefaultSimpleMessageProcessor() {
		messageProcessors = new ArrayList<>();
	}
	
	public void setMessageProcessors(List<IMessageProcessor> messageProcessors) {
		this.messageProcessors = messageProcessors;
		Collections.sort(messageProcessors, new OrderComparator<>());
	}
	
	public void addMessageProcessor(IMessageProcessor messageProcessor) {
		if (!messageProcessors.contains(messageProcessor)) {			
			messageProcessors.add(messageProcessor);
			Collections.sort(messageProcessors, new OrderComparator<>());
		}
	}

	@Override
	public boolean process(IProcessingContext context, Message message) {
		for (IMessageProcessor messageProcessor : messageProcessors) {
			if (messageProcessor.process(context, message))
				return true;
		}
		
		return false;
	}
}
