package org.sagebionetworks.repo.web.service;

import java.util.List;

import org.sagebionetworks.repo.manager.StorageUsageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class StorageUsageServiceImpl implements StorageUsageService {

	@Autowired
	private UserManager userManager;

	@Autowired
	private StorageUsageManager storageUsageManager;

	@Override
	public StorageUsageSummaryList getStorageUsage(String currUserId, String userId, List<StorageUsageDimension> dList)
			throws UnauthorizedException, DatastoreException {

		// this will throw UnauthorizedException
		checkUserAuthorization(currUserId, userId);

		StorageUsageSummaryList results = storageUsageManager.getStorageUsage(userId, dList);
		return results;
	}

	@Override
	public PaginatedResults<StorageUsage> getStorageUsage(String currUserId, String userId,
			Integer offset, Integer limit, String urlPath)
			throws UnauthorizedException, DatastoreException {

		// this will throw UnauthorizedException
		checkUserAuthorization(currUserId, userId);

		QueryResults<StorageUsage> queryResults = storageUsageManager.getStorageUsage(userId, offset, limit);
		PaginatedResults<StorageUsage> results = new PaginatedResults<StorageUsage>(urlPath, 
				queryResults.getResults(), queryResults.getTotalNumberOfResults(), 
				offset, limit, null, true);
		return results;
	}

	/**
	 * Whether the current user is allowed to view storage usage for the specified user.
	 *
	 * @throws UnauthorizedException When current user is not authorized to view another user
	 */
	private void checkUserAuthorization(String currUserId, String userId) throws DatastoreException, UnauthorizedException {
		if (currUserId == null) {
			throw new NullPointerException();
		}
		if (userId == null) {
			throw new NullPointerException();
		}
		if (!currUserId.equals(userId)) {
			UserInfo currUserInfo = null;
			try {
				currUserInfo = userManager.getUserInfo(currUserId);
			} catch (NotFoundException e) {
				throw new UnauthorizedException("Only administrator is allowed to view other user's storage usage.");
			}
			if (currUserInfo == null || !currUserInfo.isAdmin()) {
				throw new UnauthorizedException("Only administrator is allowed to view other user's storage usage.");
			}
		}
	}
}
