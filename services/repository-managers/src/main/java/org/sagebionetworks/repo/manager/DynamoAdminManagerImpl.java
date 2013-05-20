package org.sagebionetworks.repo.manager;

import org.sagebionetworks.dynamo.dao.DynamoAdminDao;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class DynamoAdminManagerImpl implements DynamoAdminManager {

	@Autowired
	private DynamoAdminDao dynamoAdminDao;

	@Autowired
	private UserManager userManager;

	@Override
	public void clear(String userName, String tableName, String hashKeyName, String rangeKeyName)
			throws UnauthorizedException, DatastoreException, NotFoundException {

		if (userName == null || userName.isEmpty()) {
			throw new IllegalArgumentException("User name cannot be null or empty.");
		}
		if (tableName == null || tableName.isEmpty()) {
			throw new IllegalArgumentException("Table name cannot be null or empty.");
		}
		if (hashKeyName == null || hashKeyName.isEmpty()) {
			throw new IllegalArgumentException("Hash key name cannot be null or empty.");
		}
		if (rangeKeyName == null || rangeKeyName.isEmpty()) {
			throw new IllegalArgumentException("Range key name cannot be null or empty.");
		}

		UserInfo userInfo = userManager.getUserInfo(userName);
		if (!userInfo.isAdmin()) {
			throw new UnauthorizedException("User " + userName + " is not an administrator.");
		}

		dynamoAdminDao.clear(tableName, hashKeyName, rangeKeyName);
	}
}
