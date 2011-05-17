/**
 * 
 */
package org.sagebionetworks.repo.model;

import java.util.Collection;

import org.sagebionetworks.repo.web.NotFoundException;

/**
 * 
 * TODO:  migrate this to a more appropriate package
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

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.UserDAO#getUser(java.lang.String)
	 */
	@Override
	public User getUser(String userName) throws DatastoreException {
		User user = new User();
		user.setUserId(userName);
		user.setId(userName);  //  i.e. user name == user id
		return user;
	}

}
