package com.thefirstlineofcode.granite.xeps.msgoffline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.oxm.IOxmFactory;
import com.thefirstlineofcode.basalt.oxm.OxmService;
import com.thefirstlineofcode.basalt.oxm.coc.CocTranslatorFactory;
import com.thefirstlineofcode.basalt.oxm.translators.im.MessageTranslatorFactory;
import com.thefirstlineofcode.basalt.xeps.delay.Delay;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;
import com.thefirstlineofcode.basalt.xmpp.datetime.DateTime;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.granite.framework.core.annotations.BeanDependency;
import com.thefirstlineofcode.granite.framework.core.config.IConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.event.IEventListener;
import com.thefirstlineofcode.granite.framework.im.IOfflineMessageStore;
import com.thefirstlineofcode.granite.framework.im.OfflineMessageEvent;

public class OfflineMessageListener implements IEventListener<OfflineMessageEvent>,
			IConfigurationAware {
	private static final Logger logger = LoggerFactory.getLogger(OfflineMessageListener.class);
	
	private static final String CONFIGURATION_KEY_DISABLED = "disabled";
	
	private boolean disabled;
	
	@BeanDependency
	private IOfflineMessageStore offlineMessageStore;
	
	private IOxmFactory oxmFactory = OxmService.createMinimumOxmFactory();
	
	public OfflineMessageListener() {
		oxmFactory.register(
				Message.class,
				new MessageTranslatorFactory()
		);
		oxmFactory.register(
				Delay.class,
				new CocTranslatorFactory<>(
						Delay.class
				)
		);
	}
	
	@Override
	public void process(IEventContext context, OfflineMessageEvent event) {
		if (disabled)
			return;
		
		JabberId jid = event.getContact();
		Message message = event.getMessage();
		
		if (message.getId() == null) {
			message.setId(Stanza.generateId("om"));
		}
		
		message.setObject(new Delay(message.getFrom(), new DateTime()));
		
		if (message.getFrom() == null) {
			message.setFrom(event.getUser());
		}
		
		try {
			offlineMessageStore.save(jid, message.getId(), oxmFactory.translate(message));
		} catch (Exception e) {
			logger.error("Can't save offline message.", e);
		}
		
	}

	@Override
	public void setConfiguration(IConfiguration configuration) {
		disabled = configuration.getBoolean(CONFIGURATION_KEY_DISABLED, false);
	}

}
