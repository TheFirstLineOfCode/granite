package com.thefirstlineofcode.granite.xeps.ibr;

import com.thefirstlineofcode.basalt.xeps.ibr.IqRegister;

public interface IRegistrar {
	IqRegister getRegistrationForm();
	void register(IqRegister iqRegister);
	void remove(String username);
}
