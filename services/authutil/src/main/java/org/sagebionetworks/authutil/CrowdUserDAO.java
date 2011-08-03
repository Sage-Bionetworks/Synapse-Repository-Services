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
		DateFormat df = new SimpleDateFormat(AuthUtilConstants.DATE_FORMAT);
		if (dto.getCreationDate()!=null) userAttributes.put(AuthUtilConstants.CREATION_DATE_FIELD, Arrays.asList(new String[]{df.format(dto.getCreationDate())}));
		if (dto.getIamUserId()!=null) userAttributes.put(IAM_USER_ID_FIELD, Arrays.asList(new String[]{dto.getIamUserId()}));
		if (dto.getIamAccessId()!=null) userAttributes.put(IAM_ACCESS_ID_FIELD, Arrays.asList(new String[]{dto.getIamAccessId()}));
		if (dto.getIamSecretKey()!=null) userAttributes.put(IAM_SECRET_KEY_FIELD, Arrays.asList(new String[]{dto.getIamSecretKey()}));
		try {
			(new CrowdAuthUtil()).setUserAttributes(dto.getUserId(), userAttributes);
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

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.UserDAO#getUser(java.lang.String)
	 */
	@Override
	public User getUser(String userName) throws DatastoreException, NotFoundException {
		User user = new User();
		user.setUserId(userName);
		user.setId(userName);  //  i.e. user name == user id
		if (AuthUtilConstants.ANONYMOUS_USER_ID.equals(userName)) return user;
		Map<String,Collection<String>> userAttrValues = null;
		try {
			userAttrValues = (new CrowdAuthUtil()).getUserAttributes(userName);
		} catch (NotFoundException nfe) {
			throw nfe;
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
		Collection<String> values;
		values = userAttrValues.get(AuthUtilConstants.CREATION_DATE_FIELD);

		DateFormat df = new SimpleDateFormat(AuthUtilConstants.DATE_FORMAT);
		
		if (values!=null && values.size()>0) {
			String dateString = values.iterator().next();
			if (dateString!=null && dateString.length()>0) {
				try {
					Date date = df.parse(dateString);
					user.setCreationDate(date);
				} catch (ParseException dfe) {
					throw new DatastoreException(dfe);
				}
			}
		}
		values = userAttrValues.get(IAM_USER_ID_FIELD);
		if (values!=null && values.size()>0) user.setIamUserId(values.iterator().next());
		values = userAttrValues.get(IAM_ACCESS_ID_FIELD);
		if (values!=null && values.size()>0) user.setIamAccessId(values.iterator().next());
		values = userAttrValues.get(IAM_SECRET_KEY_FIELD);
		if (values!=null && values.size()>0) user.setIamSecretKey(values.iterator().next());
		
		return user;
	}
	
	@Override
	public Collection<String> getUserGroupNames(String userName) throws NotFoundException, DatastoreException {
		try {
			return (new CrowdAuthUtil()).getUsersGroups(userName);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	
}
