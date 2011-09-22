/**
 * 
 */
package org.sagebionetworks.authutil;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
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
		if (dto.getCreationDate()!=null) userAttributes.put(AuthUtilConstants.CREATION_DATE_FIELD, 
				Arrays.asList(new String[]{new DateTime(dto.getCreationDate()).toString()}));
		if (dto.getIamUserId()!=null) userAttributes.put(IAM_USER_ID_FIELD, Arrays.asList(new String[]{dto.getIamUserId()}));
		if (dto.getIamAccessId()!=null) userAttributes.put(IAM_ACCESS_ID_FIELD, Arrays.asList(new String[]{dto.getIamAccessId()}));
		if (dto.getIamSecretKey()!=null) userAttributes.put(IAM_SECRET_KEY_FIELD, Arrays.asList(new String[]{dto.getIamSecretKey()}));
		if (dto.getIamCredsExpirationDate()!=null) userAttributes.put(IAM_EXPIRATION_FIELD, 
				Arrays.asList(new String[]{new DateTime(dto.getIamCredsExpirationDate()).toString()}));
		try {
			crowdAuthUtil().setUserAttributes(dto.getUserId(), userAttributes);
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
	
	private static final String IAM_USER_ID_FIELD = "iamUserId";
	private static final String IAM_ACCESS_ID_FIELD = "iamAccessId";
	private static final String IAM_SECRET_KEY_FIELD = "iamSecretKey";
	private static final String IAM_EXPIRATION_FIELD = "iamCredsExpirationDate";
	
	private CrowdAuthUtil cau = null;
	
	private CrowdAuthUtil crowdAuthUtil() {
		if (cau==null) cau = new CrowdAuthUtil();
		return cau;
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
			userAttrValues = crowdAuthUtil().getUserAttributes(userName);
		} catch (NotFoundException nfe) {
			throw nfe;
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
		Collection<String> values;
		values = userAttrValues.get(AuthUtilConstants.CREATION_DATE_FIELD);
		if (values!=null && values.size()>0) user.setCreationDate(new DateTime(values.iterator().next()).toDate());
		values = userAttrValues.get(IAM_USER_ID_FIELD);
		if (values!=null && values.size()>0) user.setIamUserId(values.iterator().next());
		values = userAttrValues.get(IAM_ACCESS_ID_FIELD);
		if (values!=null && values.size()>0) user.setIamAccessId(values.iterator().next());
		values = userAttrValues.get(IAM_SECRET_KEY_FIELD);
		if (values!=null && values.size()>0) user.setIamSecretKey(values.iterator().next());
		values = userAttrValues.get(IAM_EXPIRATION_FIELD);
		if (values!=null && values.size()>0) user.setIamCredsExpirationDate(new DateTime(values.iterator().next()).toDate());
		
		return user;
	}
	
	@Override
	public Collection<String> getUserGroupNames(String userName) throws NotFoundException, DatastoreException {
		try {
			return crowdAuthUtil().getUsersGroups(userName);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	
}
