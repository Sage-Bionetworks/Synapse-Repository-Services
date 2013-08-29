package org.sagebionetworks.repo.manager;

import java.util.HashMap;
import java.util.Map;

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

	private String create(User dto) throws DatastoreException,
			InvalidModelException {
		if (dto.getUserId() == null) {
			throw new NullPointerException("user-id required!");
		}
		// make the user name also the object's id
		dto.setId(dto.getUserId()); 
		map.put(dto.getUserId(), dto);
		return dto.getId();
	}
	
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
			user = map.get(userName);
			if (user==null) throw new NotFoundException("Failed to create "+userName);
		}
		return user;
	}
}
