package com.thefirstlineofcode.granite.xeps.ibr;

import com.thefirstlineofcode.basalt.xeps.ibr.IqRegister;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.Conflict;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.NotAcceptable;
import com.thefirstlineofcode.granite.framework.adf.core.AdfComponentService;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentService;
import com.thefirstlineofcode.granite.framework.core.adf.IApplicationComponentServiceAware;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.annotations.Dependency;
import com.thefirstlineofcode.granite.framework.core.auth.Account;
import com.thefirstlineofcode.granite.framework.core.auth.IAccountManager;

@Component("default.registrar")
public class Registrar implements IRegistrar, IApplicationComponentServiceAware {
	private IAccountManager accountManager;
	
	@Dependency("registration.strategy")
	private IRegistrationStrategy strategy;
	
	@Override
	public IqRegister getRegistrationForm() {
		return strategy.getRegistrationForm();
	}

	@Override
	public void register(IqRegister iqRegister) {
		Account account;
		try {
			account = strategy.convertToAccount(iqRegister);
		} catch (MalformedRegistrationInfoException e) {
			throw new ProtocolException(new NotAcceptable());
		}
		
		if (accountManager.exists(account.getName()))
			throw new ProtocolException(new Conflict());
		
		accountManager.add(account);
	}
	
	@Override
	public void remove(String username) {
		accountManager.remove(username);
	}

	@Override
	public void setApplicationComponentService(IApplicationComponentService appComponentService) {
		accountManager = ((AdfComponentService)appComponentService).getApplicationContext().getBean(IAccountManager.class);
	}
	
}
