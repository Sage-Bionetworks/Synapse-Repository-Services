package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of UserDAO which gets information from RDS
 */
public class DBOUserDAOImpl implements UserDAO {
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private UserProfileDAO userProfileDAO;

	@Override
	public User getUser(String userName) throws DatastoreException,
			NotFoundException {
		User user = new User();
		user.setUserId(userName);
		user.setId(userName); // i.e. username == user id
		
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userName)) {
			return user;
		}
		
		UserGroup ug = userGroupDAO.findGroup(userName, true);
		user.setCreationDate(ug.getCreationDate());
		
		UserProfile up = userProfileDAO.get(ug.getId());
		user.setFname(up.getFirstName());
		user.setLname(up.getLastName());
		user.setDisplayName(up.getDisplayName());
		user.setAgreesToTermsOfUse(up.getAgreesToTermsOfUse() != null && 
				up.getAgreesToTermsOfUse() >= AuthorizationConstants.MOST_RECENT_TERMS_OF_USE);
		
		return user;
	}

	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		throw new UnsupportedOperationException();
	}
	
}
