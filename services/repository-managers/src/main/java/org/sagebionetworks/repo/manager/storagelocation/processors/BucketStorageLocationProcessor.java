package org.sagebionetworks.repo.manager.storagelocation.processors;

import org.sagebionetworks.repo.manager.storagelocation.StorageLocationProcessor;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.project.BucketStorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

import com.google.common.net.InternetDomainName;

@Service
public class BucketStorageLocationProcessor implements StorageLocationProcessor<BucketStorageLocationSetting> {

	@Override
	public boolean supports(Class<? extends StorageLocationSetting> storageLocationClass) {
		return BucketStorageLocationSetting.class.isAssignableFrom(storageLocationClass);
	}
	
	@Override
	public void beforeCreate(UserInfo userInfo, BucketStorageLocationSetting storageLocation) {
		ValidateArgument.required(storageLocation.getBucket(), "The bucket name");
		// A valid bucket name must also be a valid domain name
		ValidateArgument.requirement(InternetDomainName.isValid(storageLocation.getBucket()), "Invalid bucket name.");
	}
	
}
