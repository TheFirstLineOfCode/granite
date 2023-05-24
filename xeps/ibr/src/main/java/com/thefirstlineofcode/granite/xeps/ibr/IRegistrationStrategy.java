package com.thefirstlineofcode.granite.xeps.ibr;

import com.thefirstlineofcode.basalt.xeps.ibr.IqRegister;
import com.thefirstlineofcode.granite.framework.core.auth.Account;

public interface IRegistrationStrategy {
	IqRegister getRegistrationForm();
	Account convertToAccount(IqRegister iqRegister) throws MalformedRegistrationInfoException;
}
