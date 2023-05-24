package com.thefirstlineofcode.granite.lite.auth;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.thefirstlineofcode.granite.framework.core.auth.Account;
import com.thefirstlineofcode.granite.framework.core.auth.IAuthenticator;

@Transactional
@Component
public class Authenticator implements IAuthenticator {
	@Autowired
	private SqlSession sqlSession;

	@Override
	public Object getCredentials(Object principal) {
		AccountMapper mapper = sqlSession.getMapper(AccountMapper.class);
		
		Account account = mapper.selectByName((String)principal);
		if (account != null)
			return account.getPassword();
		
		return null;
	}

	@Override
	public boolean exists(Object principal) {
		AccountMapper mapper = sqlSession.getMapper(AccountMapper.class);
		int count = mapper.selectCountByName((String)principal);
		
		return count != 0;
	}

}
