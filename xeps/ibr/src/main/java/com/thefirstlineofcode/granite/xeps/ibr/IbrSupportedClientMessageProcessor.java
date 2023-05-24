package com.thefirstlineofcode.granite.xeps.ibr;

import java.util.List;

import com.thefirstlineofcode.basalt.xeps.ibr.Register;
import com.thefirstlineofcode.basalt.xeps.ibr.oxm.RegisterTranslatorFactory;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Feature;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Features;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.config.IConfiguration;
import com.thefirstlineofcode.granite.framework.core.connection.IClientConnectionContext;
import com.thefirstlineofcode.granite.pipeline.stages.stream.IStreamNegotiant;
import com.thefirstlineofcode.granite.pipeline.stages.stream.negotiants.InitialStreamNegotiant;
import com.thefirstlineofcode.granite.pipeline.stages.stream.negotiants.ResourceBindingNegotiant;
import com.thefirstlineofcode.granite.pipeline.stages.stream.negotiants.SaslNegotiant;
import com.thefirstlineofcode.granite.pipeline.stages.stream.negotiants.SessionEstablishmentNegotiant;
import com.thefirstlineofcode.granite.pipeline.stages.stream.negotiants.TlsNegotiant;
import com.thefirstlineofcode.granite.stream.standard.StandardClientMessageProcessor;

@Component("ibr.supported.client.message.processor")
public class IbrSupportedClientMessageProcessor extends StandardClientMessageProcessor {
	private static final int DEFAULT_REGISTRAR_TIMEOUT_CHECK_INTERVAL = 1000;
	private static final String CONFIGURATION_KEY_REGISTRAR_TIMEOUT_CHECK_INTERVAL = "registrar.timeout.check.interval";
	
	private int registrarTimeoutCheckInterval;
	
	@Dependency("registrar")
	private IRegistrar registrar;
	
	@Override
	protected IStreamNegotiant createNegotiant() {
		if (tlsRequired) {
			IStreamNegotiant initialStream = new InitialStreamNegotiant(hostName,
					getInitialStreamNegotiantAdvertisements());
			
			IStreamNegotiant tls = new IbrSupportedTlsNegotiant(hostName, tlsRequired,
					getTlsNegotiantAdvertisements());
			
			IStreamNegotiant ibrAfterTls = new IbrNegotiant(hostName,
					getTlsNegotiantAdvertisements(), registrar, registrarTimeoutCheckInterval);
			
			IStreamNegotiant sasl = new SaslNegotiant(hostName,
					saslSupportedMechanisms, saslAbortRetries, saslFailureRetries,
					getSaslNegotiantFeatures(), authenticator);
			
			IStreamNegotiant resourceBinding = new ResourceBindingNegotiant(connectionManager,
					hostName, sessionManager, router);
			IStreamNegotiant sessionEstablishment = new SessionEstablishmentNegotiant(router,
					sessionManager, eventFirer, sessionListenerDelegate);
			
			resourceBinding.setNext(sessionEstablishment);
			sasl.setNext(resourceBinding);
			ibrAfterTls.setNext(sasl);
			tls.setNext(ibrAfterTls);
			initialStream.setNext(tls);
			
			return initialStream;
		} else {
			IStreamNegotiant initialStream = new IbrSupportedInitialStreamNegotiant(hostName,
					getInitialStreamNegotiantAdvertisements());
			
			IStreamNegotiant ibrBeforeTls = new IbrNegotiant(hostName,
					getInitialStreamNegotiantAdvertisements(), registrar, registrarTimeoutCheckInterval);
			
			IStreamNegotiant tls = new IbrSupportedTlsNegotiant(hostName, tlsRequired,
					getTlsNegotiantAdvertisements());
			
			IStreamNegotiant ibrAfterTls = new IbrNegotiant(hostName,
					getTlsNegotiantAdvertisements(), registrar, registrarTimeoutCheckInterval);
			
			IStreamNegotiant sasl = new SaslNegotiant(hostName,
					saslSupportedMechanisms, saslAbortRetries, saslFailureRetries,
					getSaslNegotiantFeatures(), authenticator);
			
			IStreamNegotiant resourceBinding = new ResourceBindingNegotiant(connectionManager,
					hostName, sessionManager, router);
			IStreamNegotiant sessionEstablishment = new SessionEstablishmentNegotiant(router,
					sessionManager, eventFirer, sessionListenerDelegate);
			
			resourceBinding.setNext(sessionEstablishment);
			sasl.setNext(resourceBinding);
			ibrAfterTls.setNext(sasl);
			tls.setNext(ibrAfterTls);
			ibrBeforeTls.setNext(tls);
			initialStream.setNext(ibrBeforeTls);
			
			return initialStream;
		}
		
	}
	
	private static class IbrSupportedInitialStreamNegotiant extends InitialStreamNegotiant {
		static {
			oxmFactory.register(Register.class, new RegisterTranslatorFactory());
		}
		
		public IbrSupportedInitialStreamNegotiant(String domainName, List<Feature> features) {
			super(domainName, features);
			features.add(new Register());
		}
	}
	
	private static class IbrSupportedTlsNegotiant extends TlsNegotiant {
		static {
			oxmFactory.register(Register.class, new RegisterTranslatorFactory());
		}

		public IbrSupportedTlsNegotiant(String domainName, boolean tlsRequired, List<Feature> features) {
			super(domainName, tlsRequired, features);
		}
		
		@Override
		protected Features getAvailableFeatures(IClientConnectionContext context) {
			Features features = super.getAvailableFeatures(context);
			
			if (context.getAttribute(IbrNegotiant.KEY_IBR_REGISTERED) == null) {
				features.getFeatures().add(new Register());
			}
			
			return features;
		}
	}
	
	@Override
	public void setConfiguration(IConfiguration configuration) {
		super.setConfiguration(configuration);
		
		registrarTimeoutCheckInterval = configuration.getInteger(
				CONFIGURATION_KEY_REGISTRAR_TIMEOUT_CHECK_INTERVAL,
				DEFAULT_REGISTRAR_TIMEOUT_CHECK_INTERVAL
		);
	}
}
