package com.thefirstlineofcode.granite.stream.standard;

import java.io.IOException;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslFilter;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.granite.framework.core.connection.IClientConnectionContext;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessage;
import com.thefirstlineofcode.granite.framework.core.session.ValueWrapper;
import com.thefirstlineofcode.granite.pipeline.stages.stream.security.TlsEnvironment;

public class SocketConnectionContext extends AbstractSocketConnectionContext implements IClientConnectionContext {
	public static final String STREAM_ID = "socket";
	
	private static final String NAME_SSL_FILTER = "SSLFilter";
	
	private static final Object lock = new Object();
	private static SslFilter globalSslFilter;
	
	private TlsParameter tlsParameter;
	
	public SocketConnectionContext(IoSession session, String localNodeId) {
		super(session, localNodeId);
		this.tlsParameter = (TlsParameter)session.getAttribute(TlsParameter.KEY_TLS_PARAMETER);
	}

	@Override
	public boolean isTlsSupported() {
		return true;
	}

	@Override
	public boolean isTlsStarted() {
		return getSslFilter().isSslStarted(session);
	}

	@Override
	public void startTls() {
		SslFilter sslFilter = getSslFilter();
		session.getFilterChain().addFirst(NAME_SSL_FILTER, sslFilter);
		session.setAttribute(SslFilter.DISABLE_ENCRYPTION_ONCE, true);
	}

	private SslFilter getSslFilter() {
		if (globalSslFilter != null)
			return globalSslFilter;
		
		synchronized (lock) {
			if (globalSslFilter != null)
				return globalSslFilter;
			
			TlsEnvironment tlsEnvironment = new TlsEnvironment(tlsParameter.getSecurityDir(), tlsParameter.getHostName());
			try {
				tlsEnvironment.init();
			} catch (SecurityException e) {
				throw e;
			} catch (IOException e) {
				throw new SecurityException("io error", e);
			}
			
			globalSslFilter = new SslFilter(tlsEnvironment.getSslContext(), false);
			
			globalSslFilter.setEnabledProtocols(new String[] {
				"TLSv1",
				"TLSv1.1",
				"TLSv1.2"
			});
			globalSslFilter.setEnabledCipherSuites(new String[] {
				"SSL_RSA_WITH_3DES_EDE_CBC_SHA",
				"TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
				"TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
				"TLS_RSA_WITH_AES_256_CBC_SHA",
				"TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
				"TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
				"TLS_RSA_WITH_AES_128_CBC_SHA"
			});
			
			return globalSslFilter;
		}
	}

	@Override
	public JabberId getJid() {
		return (JabberId)session.getAttribute(IMessage.KEY_SESSION_JID);
	}

	@Override
	public String getStreamId() {
		return STREAM_ID;
	}

	@Override
	public <T> T setAttribute(Object key, ValueWrapper<T> wrapper) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getLocalNodeId() {
		return localNodeId;
	}

}
