package com.thefirstlineofcode.granite.pipeline.stages.stream.negotiants;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;

import com.thefirstlineofcode.granite.framework.core.auth.IAuthenticator;
import com.thefirstlineofcode.granite.framework.core.auth.PrincipalNotFoundException;

public class DefaultAuthenticationCallbackHandler implements CallbackHandler {
	private String password;
	private PasswordCallback passwordCallback;
	private IAuthenticator authenticator;
	
	public DefaultAuthenticationCallbackHandler(IAuthenticator authenticator) {
		this.authenticator = authenticator;
	}

	@Override
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		for (Callback callback : callbacks) {
			if (callback instanceof NameCallback) {
				NameCallback nameCallback = (NameCallback)callback;
				if (authenticator.exists(nameCallback.getDefaultName())) {
					nameCallback.setName(nameCallback.getDefaultName());
					try {
						password = (String)authenticator.getCredentials(nameCallback.getDefaultName());
					} catch (PrincipalNotFoundException e) {
						password = null;
					}
				}
			} else if (callback instanceof PasswordCallback) {
				passwordCallback = (PasswordCallback)callback;
			} else if (callback instanceof AuthorizeCallback) {
				AuthorizeCallback authorizeCallback = (AuthorizeCallback)callback;
				if (authorizeCallback.getAuthenticationID().equals(authorizeCallback.getAuthorizationID())) {
					authorizeCallback.setAuthorized(true);
				}
			}
		}
		
		if (passwordCallback != null) {
			passwordCallback.setPassword(password == null ? null : password.toCharArray());
		}
	}

}
