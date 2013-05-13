package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.web.NotFoundException;

public class TestUserDAO implements UserDAO {
	
	public static final String TEST_GROUP_NAME = "test-group";
	public static final String TEST_USER_NAME = "test-user@sagebase.org";
	public static final String ADMIN_USER_NAME = "admin@sagebase.org";
	public static final String MIGRATION_USER_NAME = "migrationAdmin@sagebase.org";
	
	private Map<String,User> map = new HashMap<String,User>(); // maps userId to User

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.BaseDAO#create(org.sagebionetworks.repo.model.Base)
	 */
	@Override
	public String create(User dto) throws DatastoreException,
			InvalidModelException {
		if (dto.getUserId()==null) throw new NullPointerException("user-id required!");
		dto.setId(dto.getUserId()); // make the user name also the object's id
		

		map.put(dto.getUserId(), dto);
		return dto.getId();
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.BaseDAO#get(java.lang.String)
	 */
	@Override
	public User get(String id) throws DatastoreException, NotFoundException {
		return map.get(id);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.BaseDAO#getAll()
	 */
	@Override
	public Collection<User> getAll() throws DatastoreException {
		return map.values();
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.BaseDAO#update(org.sagebionetworks.repo.model.Base)
	 */
	@Override
	public void update(User dto) throws DatastoreException,
			InvalidModelException, NotFoundException {
		map.put(dto.getId(), dto);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.BaseDAO#delete(java.lang.String)
	 */
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		map.remove(id);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.UserDAO#getUser(java.lang.String)
	 */
	@Override
	public User getUser(String userName) throws DatastoreException, NotFoundException {
		User user = map.get(userName);
		if (user==null) {
			user = new User();
			user.setUserId(userName);
			user.setAgreesToTermsOfUse(true);
			try {
				create(user);
			} catch (InvalidModelException ime) {
				throw new IllegalStateException(ime);
			}
			user = get(userName);
			if (user==null) throw new NotFoundException("Failed to create "+userName);
		}
		return user;
	}
	
	@Override
	public Collection<String> getUserGroupNames(String userName) {
		Collection<String> ans = new HashSet<String>();
		if (ADMIN_USER_NAME.equals(userName)) {
			ans.add(AuthorizationConstants.ADMIN_GROUP_NAME);
		} else if (MIGRATION_USER_NAME.equals(userName)) {
			ans.add(AuthorizationConstants.ADMIN_GROUP_NAME);
		}  else if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userName)) {
			// not in any group
		} else {
			ans.add(TEST_GROUP_NAME);
		}
		return ans;
	}	

}
