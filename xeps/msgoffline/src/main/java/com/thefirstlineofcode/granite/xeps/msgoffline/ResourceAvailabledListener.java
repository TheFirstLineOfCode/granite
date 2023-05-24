package com.thefirstlineofcode.granite.xeps.msgoffline;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.annotations.BeanDependency;
import com.thefirstlineofcode.granite.framework.core.config.IConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventListener;
import com.thefirstlineofcode.granite.framework.im.IOfflineMessageStore;
import com.thefirstlineofcode.granite.framework.im.OfflineMessage;
import com.thefirstlineofcode.granite.framework.im.ResourceAvailabledEvent;

public class ResourceAvailabledListener implements IConfigurationAware,
			IEventListener<ResourceAvailabledEvent> {
	private static final String CONFIGURATION_KEY_DISABLED = "disabled";
	
	private boolean disabled;

	@BeanDependency
	private IOfflineMessageStore offlineMessageStore;
		
	@Override
	public void process(IEventContext context, ResourceAvailabledEvent event) {
		JabberId resource = event.getJid();
		
		processOfflineMessages(context, resource, resource);
		processOfflineMessages(context, resource.getBareId(), resource);
	}

	private void processOfflineMessages(IEventContext context, JabberId target, JabberId resource) {
		if (disabled)
			return;
		
		List<String> removes = new ArrayList<>();
		Iterator<OfflineMessage> offlineMessages = offlineMessageStore.iterator(target);
		while (offlineMessages.hasNext()) {
			OfflineMessage offlineMessage = offlineMessages.next();
			removes.add(offlineMessage.getMessageId());
			
			context.write(resource, offlineMessage.getMessage());
		}
		
		for (String remove : removes) {
			offlineMessageStore.remove(target, remove);
		}
	}

	@Override
	public void setConfiguration(IConfiguration configuration) {
		disabled = configuration.getBoolean(CONFIGURATION_KEY_DISABLED, false);
	}

}
