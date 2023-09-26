package com.thefirstlineofcode.granite.cluster.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.thefirstlineofcode.granite.framework.core.auth.Account;
import com.thefirstlineofcode.granite.framework.core.auth.IAccountManager;
import com.thefirstlineofcode.granite.framework.core.auth.IAuthenticator;
import com.thefirstlineofcode.granite.framework.core.auth.PrincipalNotFoundException;

@Component
public class Authenticator implements IAuthenticator {
	@Autowired
	private IAccountManager accountManager;
	
	@Override
	public Object getCredentials(Object principal) throws PrincipalNotFoundException {
		Account account = accountManager.get((String)principal);
		if (account == null)
			return new PrincipalNotFoundException(String.format("Account name %s wasn't existed.", principal));
		
		return account.getPassword();
	}

	@Override
	public boolean exists(Object principal) {
		return accountManager.exists((String)principal);
	}
}
