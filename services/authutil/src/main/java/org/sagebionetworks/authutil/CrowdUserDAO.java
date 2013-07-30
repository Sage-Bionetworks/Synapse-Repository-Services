/**
 * 
 */
package org.sagebionetworks.authutil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * 
 * Implementation of UserDAO which gets information from Crowd
 * 
 * @author bhoff
 *
 */
public class CrowdUserDAO implements UserDAO {
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.BaseDAO#create(org.sagebionetworks.repo.model.Base)
	 */
	@Override
	public String create(User dto) throws DatastoreException,
			InvalidModelException {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.BaseDAO#get(java.lang.String)
	 */
	@Override
	public User get(String id) throws DatastoreException, NotFoundException {
		
		return getUser(id); //  i.e. user name == user id
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.BaseDAO#getAll()
	 */
	@Override
	public Collection<User> getAll() throws DatastoreException {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.BaseDAO#update(org.sagebionetworks.repo.model.Base)
	 */
	@Override
	public void update(User dto) throws DatastoreException {
		Map<String,Collection<String>> userAttributes = new HashMap<String,Collection<String>>();
		if (dto.getCreationDate()!=null) userAttributes.put(AuthorizationConstants.CREATION_DATE_FIELD, 
				Arrays.asList(new String[]{new DateTime(dto.getCreationDate()).toString()}));
		try {
			CrowdAuthUtil.setUserAttributes(dto.getUserId(), userAttributes);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.BaseDAO#delete(java.lang.String)
	 */
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		throw new UnsupportedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.UserDAO#getUser(java.lang.String)
	 */
	@Override
	public User getUser(String userName) throws DatastoreException, NotFoundException {
		User user = new User();
		user.setUserId(userName);
		user.setId(userName);  //  i.e. user name == user id
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userName)) return user;
		Map<String,Collection<String>> userAttrValues = null;
		try {
			userAttrValues = CrowdAuthUtil.getUserAttributes(userName);
		} catch (NotFoundException nfe) {
			throw nfe;
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
		Collection<String> values;
		values = userAttrValues.get(AuthorizationConstants.CREATION_DATE_FIELD);
		if (values!=null && values.size()>0) user.setCreationDate(new DateTime(values.iterator().next()).toDate());
		
		values = userAttrValues.get(AuthorizationConstants.ACCEPTS_TERMS_OF_USE_ATTRIBUTE);
		if (values!=null && values.size()>0) user.setAgreesToTermsOfUse(Boolean.parseBoolean(values.iterator().next()));
		
		org.sagebionetworks.repo.model.auth.User authUser = null;
		try { 
			authUser = CrowdAuthUtil.getUser(userName);
		}catch (IOException e) {
			throw new DatastoreException(e);
		}
		user.setFname(authUser.getFirstName());
		user.setLname(authUser.getLastName());
		user.setDisplayName(authUser.getDisplayName());
		return user;
	}
	
	@Override
	public Collection<String> getUserGroupNames(String userName) throws NotFoundException, DatastoreException {
		try {
			return CrowdAuthUtil.getUsersGroups(userName);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	@Override
	public long getCount() throws DatastoreException {
		throw new UnsupportedOperationException();
	}

	
}
