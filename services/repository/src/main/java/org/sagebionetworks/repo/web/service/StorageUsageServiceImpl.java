package org.sagebionetworks.repo.web.service;

import java.util.List;

import org.sagebionetworks.repo.manager.AuthorizationManager;
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
	private AuthorizationManager authorizationManager;

	@Autowired
	private StorageUsageManager storageUsageManager;

	@Override
	public StorageUsageSummaryList getUsage(String currUserName,
			List<StorageUsageDimension> dimensionList) throws UnauthorizedException, DatastoreException {

		if (currUserName == null || currUserName.isEmpty()) {
			throw new IllegalArgumentException("Current user name cannot be null or empty.");
		}

		boolean isAdmin = isAdmin(currUserName);
		if (!isAdmin) {
			throw new UnauthorizedException("Only administrators are allowed.");
		}

		validateDimensionList(dimensionList);

		StorageUsageSummaryList results = storageUsageManager.getUsage(dimensionList);
		return results;
	}

	@Override
	public StorageUsageSummaryList getUsageForUser(String currUserName, String userId,
			List<StorageUsageDimension> dimensionList)
			throws UnauthorizedException, NotFoundException, DatastoreException {

		if (currUserName == null || currUserName.isEmpty()) {
			throw new IllegalArgumentException("Current user name cannot be null or empty.");
		}
		if (userId == null || userId.isEmpty()) {
			throw new IllegalArgumentException("User ID cannot be null or empty.");
		}

		boolean isAdmin = isAdmin(currUserName);
		String userName = userManager.getGroupName(userId);
		checkAuthorization(isAdmin, currUserName, userName);
		validateDimensionList(dimensionList);

		StorageUsageSummaryList results = storageUsageManager.getUsageForUser(userId, dimensionList);
		return results;
	}

	@Override
	public StorageUsageSummaryList getUsageByUserInRange(String currUserName,
			Integer offset, Integer limit) throws UnauthorizedException, DatastoreException {

		if (currUserName == null || currUserName.isEmpty()) {
			throw new IllegalArgumentException("Current user name cannot be null or empty.");
		}

		boolean isAdmin = isAdmin(currUserName);
		if (!isAdmin) {
			throw new UnauthorizedException("Only administrators are allowed.");
		}

		StorageUsageSummaryList results = storageUsageManager.getUsageByUserInRange(offset, limit);
		return results;
	}

	@Override
	public PaginatedResults<StorageUsage> getUsageInRangeForUser(String currUserName, String userName,
			Integer offset, Integer limit, String urlPath)
			throws UnauthorizedException, NotFoundException, DatastoreException {

		if (currUserName == null || currUserName.isEmpty()) {
			throw new IllegalArgumentException("Current user name cannot be null or empty.");
		}
		if (userName == null || userName.isEmpty()) {
			throw new IllegalArgumentException("User name cannot be null or empty.");
		}

		boolean isAdmin = isAdmin(currUserName);
		checkAuthorization(isAdmin, currUserName, userName);

		String userId = getUserId(userName);
		QueryResults<StorageUsage> queryResults = storageUsageManager.getUsageInRangeForUser(userId, offset, limit);
		PaginatedResults<StorageUsage> results = new PaginatedResults<StorageUsage>(urlPath, 
				queryResults.getResults(), queryResults.getTotalNumberOfResults(), 
				offset, limit, null, true);
		return results;
	}

	/**
	 * Whether the current user is an administrator.
	 */
	private boolean isAdmin(String currUserName) {
		UserInfo currUserInfo = null;
		try {
			currUserInfo = userManager.getUserInfo(currUserName);
		} catch (NotFoundException e) {
			return false;
		}
		if (currUserInfo != null && currUserInfo.isAdmin()) {
			return true;
		}
		return false;
	}

	/**
	 * Whether the current user is allowed to view storage usage for the specified user.
	 *
	 * @throws UnauthorizedException When current user is not authorized to view another user
	 */
	private void checkAuthorization(boolean isAdmin, String currUserName, String userName)
			throws DatastoreException, UnauthorizedException {
		if (!currUserName.equals(userName)) {
			if (!isAdmin) {
				throw new UnauthorizedException(
						"Only administrator is allowed to view other user's storage usage.");
			}
		}
	}

	/**
	 * Some aggregating dimensions are only accessible via paginated views.
	 *
	 * @throws IllegalArgumentException When an aggregating dimension is for paginated views only.
	 */
	private void validateDimensionList(List<StorageUsageDimension> dimensionList) {
		for (StorageUsageDimension d : dimensionList) {
			if (StorageUsageDimension.USER_ID.equals(d)) {
				throw new IllegalArgumentException(StorageUsageDimension.USER_ID + " is for paginated views only.");
			}
		}
	}

	private String getUserId(String userName) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userName);
		if (userInfo == null) {
			throw new NotFoundException(userName);
		}
		return userInfo.getIndividualGroup().getId();
	}
}
