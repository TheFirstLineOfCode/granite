package com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.BadRequest;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.StanzaError;
import com.thefirstlineofcode.granite.framework.core.utils.OrderComparator;

public class DefaultIqResultProcessor implements IIqResultProcessor {
	private List<IIqResultProcessor> iqResultProcessors;
	
	public DefaultIqResultProcessor() {
		iqResultProcessors = new ArrayList<>();
	}
	
	public void setIqResultProcessors(List<IIqResultProcessor> iqResultProcessors) {
		this.iqResultProcessors = iqResultProcessors;
		Collections.sort(this.iqResultProcessors, new OrderComparator<>());
	}
	
	public void addIqResultProcessor(IIqResultProcessor iqResultProcessor) {
		if (!iqResultProcessors.contains(iqResultProcessor)) {			
			iqResultProcessors.add(iqResultProcessor);
			Collections.sort(iqResultProcessors, new OrderComparator<>());
		}
	}
	
	@Override
	public boolean processResult(IProcessingContext context, Iq iq) {
		if (iq.getType() != Iq.Type.RESULT)
			throw new ProtocolException(new BadRequest("Neither XEP nor IQ result."));
		
		if (iq.getId() == null) {
			throw new ProtocolException(new BadRequest("Null ID."));
		}
		
		for (IIqResultProcessor iqResultProcessor : iqResultProcessors) {
			if (iqResultProcessor.processResult(context, iq))
				return true;
		}
		
		return false;
	}
	
	@Override
	public boolean processError(IProcessingContext context, StanzaError error) {
		if (error.getKind() != StanzaError.Kind.IQ)
			throw new ProtocolException(new BadRequest("Not an IQ error."));
		
		if (error.getId() == null) {
			throw new ProtocolException(new BadRequest("Null ID."));
		}
		
		for (IIqResultProcessor iqResultProcessor : iqResultProcessors) {
			if (iqResultProcessor.processError(context, error))
				return true;
		}
		
		return false;
	}
	

}
