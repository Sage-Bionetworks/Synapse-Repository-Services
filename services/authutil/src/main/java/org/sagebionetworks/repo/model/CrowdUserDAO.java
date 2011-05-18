/**
 * 
 */
package org.sagebionetworks.repo.model;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * 
 * Implementation of UserDAO which gets information from Crowd
 * 
 * @author bhoff
 *
 */
public class CrowdUserDAO implements UserDAO {
	
	private CrowdAuthUtil crowdAuthUtil = new CrowdAuthUtil();

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
	public void update(User dto) throws DatastoreException,
			InvalidModelException, NotFoundException {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.BaseDAO#delete(java.lang.String)
	 */
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		throw new UnsupportedOperationException();
	}
	
	private static final String CREATION_DATE_FIELD = "creationDate";
	private static final String IAM_ACCESS_ID_FIELD = "iamAccessId";
	private static final String IAM_SECRET_KEY_FIELD = "iamSecretKey";


	// TODO: map non-existant user into 'NotFoundException'
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.UserDAO#getUser(java.lang.String)
	 */
	@Override
	public User getUser(String userName) throws DatastoreException, NotFoundException {
		User user = new User();
		user.setUserId(userName);
		user.setId(userName);  //  i.e. user name == user id
		Collection<String> userAttributes = Arrays.asList(new String[]{
			CREATION_DATE_FIELD,
			IAM_ACCESS_ID_FIELD,
			IAM_SECRET_KEY_FIELD
		});
		Map<String,Collection<String>> userAttrValues = null;
		try {
			userAttrValues = crowdAuthUtil.getUserAttributes(userName, userAttributes);
			System.out.println("CrowdUserDAO: "+userAttrValues);
		} catch (NotFoundException nfe) {
			throw nfe;
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
		Collection<String> values;
		values = userAttrValues.get(CREATION_DATE_FIELD);

//		DateFormat df = new SimpleDateFormat("yyyy-mm-dd HH:MM:SS.SSS");
		DateFormat df = new SimpleDateFormat("yyyy-mm-dd");
		
		if (values.size()>0) {
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
		values = userAttrValues.get(IAM_ACCESS_ID_FIELD);
		if (values.size()>0) user.setIamAccessId(values.iterator().next());
		values = userAttrValues.get(IAM_SECRET_KEY_FIELD);
		if (values.size()>0) user.setIamSecretKey(values.iterator().next());
		
		return user;
	}
	
}
