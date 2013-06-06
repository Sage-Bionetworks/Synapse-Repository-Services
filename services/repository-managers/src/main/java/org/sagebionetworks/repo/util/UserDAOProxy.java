package org.sagebionetworks.repo.util;

import java.util.Collection;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public class UserDAOProxy implements UserDAO, InitializingBean {
	
	@Autowired
	private UserDAO userDAOImpl;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		String implementingClassName = System.getProperty(AuthorizationConstants.USER_DAO_INTEGRATION_TEST_SWITCH);
		if (implementingClassName==null || implementingClassName.length()==0) {
			return;
		}
		userDAOImpl = (UserDAO)Class.forName(implementingClassName).newInstance();
	}
	
	@Override
	public String create(User dto) throws DatastoreException,
			InvalidModelException {
		return userDAOImpl.create(dto);
	}

	@Override
	public User get(String id) throws DatastoreException, NotFoundException {
		return userDAOImpl.get(id);
	}

	@Override
	public Collection<User> getAll() throws DatastoreException {
		return userDAOImpl.getAll();
	}

	@Override
	public void update(User dto) throws DatastoreException,
			InvalidModelException, NotFoundException, ConflictingUpdateException {
		userDAOImpl.update(dto);
	}

	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		userDAOImpl.delete(id);
	}

	@Override
	public User getUser(String userName) throws DatastoreException,
			NotFoundException {
		return userDAOImpl.getUser(userName);
	}

	@Override
	public Collection<String> getUserGroupNames(String userName)
			throws NotFoundException, DatastoreException {
		return userDAOImpl.getUserGroupNames(userName);
	}

	@Override
	public long getCount() throws DatastoreException {
		return userDAOImpl.getCount();
	}


}
