package com.thefirstlineofcode.granite.lite.xeps.disco;

import java.util.ArrayList;
import java.util.List;

import com.thefirstlineofcode.basalt.xeps.disco.DiscoInfo;
import com.thefirstlineofcode.basalt.xeps.disco.DiscoItems;
import com.thefirstlineofcode.basalt.xeps.disco.Feature;
import com.thefirstlineofcode.basalt.xeps.disco.Identity;
import com.thefirstlineofcode.basalt.xeps.disco.Item;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.BadRequest;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.ItemNotFound;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.ServiceUnavailable;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentServiceAware;
import com.thefirstlineofcode.granite.framework.core.annotations.AppComponent;
import com.thefirstlineofcode.granite.framework.core.config.IConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.framework.core.repository.IInitializable;
import com.thefirstlineofcode.granite.xeps.disco.IDiscoProcessor;
import com.thefirstlineofcode.granite.xeps.disco.IDiscoProvider;

@AppComponent("disco.processor")
public class DiscoProcessor implements IDiscoProcessor, IApplicationComponentServiceAware, IInitializable, IConfigurationAware {
	private static final String CONFIGURATION_KEY_DISABLE_ITEM_NOT_FOUND = "disable.item.not.found";
	
	private List<IDiscoProvider> discoProviders = new ArrayList<>();
	
	private IApplicationComponentService appComponentService;
	
	private boolean disableItemNotFound;
	
	@Override
	public void discoInfo(IProcessingContext context, Iq iq, JabberId jid, String node) {
		if (iq.getType() == Iq.Type.GET) {
			doDiscoInfo(context, iq, jid, node);
		} else if (iq.getType() == Iq.Type.RESULT) {
			deliverDiscoInfo(context, iq, jid, node);
		} else {
			throw new ProtocolException(new BadRequest("IQ type must be set to 'get' or 'result'."));
		}
	}

	private void deliverDiscoInfo(IProcessingContext context, Iq iq, JabberId jid, String node) {
		throw new ProtocolException(new ServiceUnavailable("Feature delivering disco info not be implemented yet."));
	}

	private void doDiscoInfo(IProcessingContext context, Iq iq, JabberId jid, String node) {
		if (iq.getType() == Iq.Type.GET) {
			doDiscoItems(context, iq, jid, node);
		} else if (iq.getType() == Iq.Type.RESULT) {
			deliverDiscoItems(context, iq, jid, node);
		} else {
			throw new ProtocolException(new BadRequest("IQ type must be set to 'get' or 'result'."));
		}
		
	}

	private void deliverDiscoItems(IProcessingContext context, Iq iq, JabberId jid, String node) {
		throw new ProtocolException(new ServiceUnavailable("Feature delivering disco items not be implemented yet."));
	}

	private void doDiscoItems(IProcessingContext context, Iq iq, JabberId jid, String node) {
		DiscoInfo discoInfo = new DiscoInfo();
		boolean itemNotFound = true;
		
		for (IDiscoProvider discoProvider : discoProviders) {
			DiscoInfo partOfDiscoInfo = discoProvider.discoInfo(context, iq, jid, node);
			if (partOfDiscoInfo != null) {
				itemNotFound = false;
				for (Identity identity : partOfDiscoInfo.getIdentities()) {
					if (!discoInfo.getIdentities().contains(identity)) {
						discoInfo.getIdentities().add(identity);
					}
				}
				
				for (Feature feature : partOfDiscoInfo.getFeatures()) {
					if (!discoInfo.getFeatures().contains(feature)) {
						discoInfo.getFeatures().add(feature);
					}
				}
				
				if (partOfDiscoInfo.getXData() != null) {
					discoInfo.setXData(partOfDiscoInfo.getXData());
				}
				
			}
		}
		
		processItemNotFound(itemNotFound);
		
		Iq result = new Iq(Iq.Type.RESULT, discoInfo, iq.getId());
		result.setFrom(jid);
		
		context.write(result);
	}

	private void processItemNotFound(boolean itemNotFound) {
		if (!itemNotFound)
			return;
		
		if (disableItemNotFound) {
			throw new ProtocolException(new ServiceUnavailable());
		} else {
			throw new ProtocolException(new ItemNotFound());
		}
	}
	
	@Override
	public void discoItems(IProcessingContext context, Iq iq, JabberId jid, String node) {
		DiscoItems discoItems = new DiscoItems();
		boolean itemNotFound = true;
		for (IDiscoProvider discoProvider : discoProviders) {
			DiscoItems partOfDiscoItems = discoProvider.discoItems(context, iq, jid, node);
			if (partOfDiscoItems != null) {
				itemNotFound = false;
				
				for (Item item : partOfDiscoItems.getItems()) {
					if (!discoItems.getItems().contains(item)) {
						discoItems.getItems().add(item);
					}
				}
				
				if (partOfDiscoItems.getSet() != null) {
					discoItems.setSet(partOfDiscoItems.getSet());
					// Single provider provides all data, so break loop.
					break;
				}
			}
		}
		
		if (node != null) {
			discoItems.setNode(node);
		}
		
		processItemNotFound(itemNotFound);
		
		Iq result = new Iq(Iq.Type.RESULT, discoItems, iq.getId());
		result.setFrom(jid);
		
		context.write(result);
	}

	@Override
	public void init() {
		List<Class<? extends IDiscoProvider>> providerClasses = appComponentService.getExtensionClasses(IDiscoProvider.class);
		if (providerClasses == null || providerClasses.size() == 0)
			return;
		
		for (Class<? extends IDiscoProvider> providerClass : providerClasses) {
			discoProviders.add(appComponentService.createExtension(providerClass));
		}
	}
	
	@Override
	public void setApplicationComponentService(IApplicationComponentService appComponentService) {
		this.appComponentService = appComponentService;
	}

	@Override
	public void setConfiguration(IConfiguration configuration) {
		disableItemNotFound = configuration.getBoolean(CONFIGURATION_KEY_DISABLE_ITEM_NOT_FOUND, true);
	}
}
