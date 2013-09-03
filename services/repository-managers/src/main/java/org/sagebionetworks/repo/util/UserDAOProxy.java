package org.sagebionetworks.repo.util;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
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
	public User getUser(String userName) throws DatastoreException,
			NotFoundException {
		return userDAOImpl.getUser(userName);
	}

	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		userDAOImpl.delete(id);
	}

}
