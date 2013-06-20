package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.StorageQuotaDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.storage.StorageQuota;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class StorageQuotaManagerImpl implements StorageQuotaManager{

	private static final int DEFAULT_QUOTA = 2000; // 2 GB

	@Autowired
	private StorageQuotaDao storageQuotaDao;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void setQuotaForUser(UserInfo currentUser, UserInfo user, int quotaInMb) {

		if (currentUser == null) {
			throw new IllegalArgumentException("Current user cannot be null.");
		}
		if (user == null) {
			throw new IllegalArgumentException("User cannot be null.");
		}
		if (quotaInMb < 0) {
			throw new IllegalArgumentException("Storage quota must be >= 0.");
		}

		if (!currentUser.isAdmin()) {
			throw new UnauthorizedException("You must be an administrator to set storage quota.");
		}

		final String userId = user.getIndividualGroup().getId();
		StorageQuota quota = storageQuotaDao.getQuota(userId);
		if (quota == null) {
			quota = new StorageQuota();
			quota.setOwnerId(userId);
		}
		quota.setQuotaInMb((long)quotaInMb);
		storageQuotaDao.setQuota(quota);
	}

	@Override
	public int getQuotaForUser(UserInfo currentUser, UserInfo user) {

		if (currentUser == null) {
			throw new IllegalArgumentException("Current user cannot be null.");
		}
		if (user == null) {
			throw new IllegalArgumentException("User cannot be null.");
		}

		if (!currentUser.isAdmin()) {
			String currUserId = currentUser.getIndividualGroup().getId();
			String userId = user.getIndividualGroup().getId();
			if (!currUserId.equals(userId)) {
				throw new UnauthorizedException("User " + currUserId
						+ " cannot access storage quota of user" + userId + ".");
			}
		}

		final String userId = user.getIndividualGroup().getId();
		StorageQuota quota = storageQuotaDao.getQuota(userId);
		if (quota == null) {
			return DEFAULT_QUOTA;
		}
		return quota.getQuotaInMb().intValue();
	}
}
