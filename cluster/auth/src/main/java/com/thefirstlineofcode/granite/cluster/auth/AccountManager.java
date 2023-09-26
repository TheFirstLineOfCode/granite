package com.thefirstlineofcode.granite.cluster.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.thefirstlineofcode.granite.framework.core.auth.Account;
import com.thefirstlineofcode.granite.framework.core.auth.IAccountManager;

@Component
@Transactional
public class AccountManager implements IAccountManager {
	private static final String FIELD_NAME_NAME = "name";
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	@Override
	public void add(Account account) {
		if (!(account instanceof D_Account))
			throw new IllegalArgumentException(String.format("Must be type of '%s'", D_Account.class.getName()));
		
		if (account.getName() == null || account.getPassword() == null)
			throw new IllegalArgumentException("Null user name or password.");
		
		mongoTemplate.save(account);
	}

	@Override
	public void remove(String name) {
		mongoTemplate.findAndRemove(
				new Query().addCriteria(Criteria.where(FIELD_NAME_NAME).is(name)),
				D_Account.class);
	}
	
	@Override
	public boolean exists(String name) {
		return mongoTemplate.count(
				new Query().addCriteria(Criteria.where(FIELD_NAME_NAME).is(name)),
				D_Account.class) != 0;
	}
	
	@Override
	public D_Account get(String name) {
		return mongoTemplate.findOne(
				new Query().addCriteria(Criteria.where(FIELD_NAME_NAME).is(name)),
				D_Account.class);
	}
	
	@Override
	public void add(String name, String password) {
		throw new UnsupportedOperationException();
	}
}
