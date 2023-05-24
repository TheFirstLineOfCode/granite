package com.thefirstlineofcode.granite.cluster.xeps.ibr;

import com.thefirstlineofcode.basalt.xeps.ibr.IqRegister;
import com.thefirstlineofcode.basalt.xeps.ibr.RegistrationField;
import com.thefirstlineofcode.basalt.xeps.ibr.RegistrationForm;
import com.thefirstlineofcode.granite.framework.core.annotations.Component;
import com.thefirstlineofcode.granite.framework.core.auth.Account;
import com.thefirstlineofcode.granite.framework.core.utils.StringUtils;
import com.thefirstlineofcode.granite.xeps.ibr.IRegistrationStrategy;
import com.thefirstlineofcode.granite.xeps.ibr.MalformedRegistrationInfoException;

@Component("cluster.registration.strategy")
public class RegistrationStrategy implements IRegistrationStrategy {

	@Override
	public IqRegister getRegistrationForm() {
		RegistrationField username = new RegistrationField("username");
		RegistrationField password = new RegistrationField("password");
		
		RegistrationForm form = new RegistrationForm();
		form.getFields().add(username);
		form.getFields().add(password);
		
		IqRegister iqRegister = new IqRegister();
		iqRegister.setRegister(form);
		
		return iqRegister;
	}

	@Override
	public Account convertToAccount(IqRegister iqRegister) throws MalformedRegistrationInfoException {
		RegistrationForm form = (RegistrationForm)iqRegister.getRegister();
		Account account = new Account();
		for (RegistrationField field : form.getFields()) {
			if ("username".equals(field.getName())) {
				account.setName(field.getValue());
			} else if ("password".equals(field.getName())) {
				account.setPassword(field.getValue());
			} else {
				// Ignore
			}
		}
		
		if (StringUtils.isEmpty(account.getName()) || StringUtils.isEmpty(account.getPassword())) {
			throw new MalformedRegistrationInfoException("Null user name or password.");
		}
		
		return account;
	}

}
