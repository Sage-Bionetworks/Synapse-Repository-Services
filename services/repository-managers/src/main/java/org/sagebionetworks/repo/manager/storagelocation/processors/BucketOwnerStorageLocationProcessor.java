package org.sagebionetworks.repo.manager.storagelocation.processors;

import org.sagebionetworks.repo.manager.storagelocation.BucketOwnerVerifier;
import org.sagebionetworks.repo.manager.storagelocation.StorageLocationProcessor;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.project.BucketOwnerStorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Order(Ordered.LOWEST_PRECEDENCE)
public class BucketOwnerStorageLocationProcessor implements StorageLocationProcessor<BucketOwnerStorageLocationSetting> {

	@Autowired
	private BucketOwnerVerifier bucketOwnerVerifier;
	
	@Override
	public boolean supports(Class<? extends StorageLocationSetting> storageLocationClass) {
		return BucketOwnerStorageLocationSetting.class.isAssignableFrom(storageLocationClass);
	}

	@Override
	public void beforeCreate(UserInfo userInfo, BucketOwnerStorageLocationSetting storageLocation) {
		bucketOwnerVerifier.verifyBucketOwnership(userInfo, storageLocation);
	}

}
