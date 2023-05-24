package com.thefirstlineofcode.granite.lite.xeps.disco;

import org.pf4j.Extension;

import com.thefirstlineofcode.basalt.xeps.disco.DiscoInfo;
import com.thefirstlineofcode.basalt.xeps.disco.DiscoItems;
import com.thefirstlineofcode.basalt.xeps.disco.Feature;
import com.thefirstlineofcode.basalt.xeps.disco.Identity;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.xeps.disco.IDiscoProvider;

@Extension
public class ImServerDiscoProvider implements IDiscoProvider, IServerConfigurationAware {
	@Dependency("standard.im.server.listener")
	private StandardImServerListener imServerListener;
	
	private JabberId serverJid;
	
	@Override
	public DiscoInfo discoInfo(IProcessingContext context, Iq iq, JabberId jid, String node) {
		if (!serverJid.equals(jid) || node != null)
			return null;
		
		DiscoInfo discoInfo = new DiscoInfo();
		
		discoInfo.getFeatures().add(new Feature("http://jabber.org/protocol/disco#info"));
		discoInfo.getFeatures().add(new Feature("http://jabber.org/protocol/disco#items"));
		
		if (imServerListener.isStandardStream()) {
			discoInfo.getFeatures().add(new Feature("jabber:client"));
			discoInfo.getFeatures().add(new Feature("urn:ietf:params:xml:ns:xmpp-streams"));
			discoInfo.getFeatures().add(new Feature("urn:ietf:params:xml:ns:xmpp-stanzas"));
			discoInfo.getFeatures().add(new Feature("urn:ietf:params:xml:ns:xmpp-tls"));
			discoInfo.getFeatures().add(new Feature("urn:ietf:params:xml:ns:xmpp-tls#c2s"));
			discoInfo.getFeatures().add(new Feature("urn:ietf:params:xml:ns:xmpp-sasl"));
			discoInfo.getFeatures().add(new Feature("urn:ietf:params:xml:ns:xmpp-sasl#c2s"));
			discoInfo.getFeatures().add(new Feature("urn:ietf:params:xml:ns:xmpp-bind"));
		}
		
		if (imServerListener.isIMServer()) {
			discoInfo.getIdentities().add(new Identity("server", "im"));
			
			discoInfo.getFeatures().add(new Feature("urn:ietf:params:xml:ns:xmpp-session"));
			discoInfo.getFeatures().add(new Feature("jabber:iq:roster"));
		}
		
		return discoInfo;
	}

	@Override
	public DiscoItems discoItems(IProcessingContext context, Iq iq, JabberId jid, String node) {
		return null;
	}

	@Override
	public void setServerConfiguration(IServerConfiguration serverConfiguration) {
		serverJid = JabberId.parse(serverConfiguration.getDomainName());
	}

}
