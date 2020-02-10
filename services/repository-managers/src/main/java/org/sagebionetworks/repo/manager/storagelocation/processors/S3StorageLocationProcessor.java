package org.sagebionetworks.repo.manager.storagelocation.processors;

import java.util.UUID;

import org.sagebionetworks.repo.manager.storagelocation.StorageLocationProcessor;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.springframework.stereotype.Service;

@Service
public class S3StorageLocationProcessor implements StorageLocationProcessor<S3StorageLocationSetting> {

	@Override
	public boolean supports(Class<? extends StorageLocationSetting> storageLocationClass) {
		return S3StorageLocationSetting.class.isAssignableFrom(storageLocationClass);
	}
	
	@Override
	public void beforeCreate(UserInfo userInfo, S3StorageLocationSetting storageLocation) {
		if (storageLocation.getBaseKey() != null) {
			throw new IllegalArgumentException("Cannot specify baseKey when creating an S3StorageLocationSetting");
		}

		if (Boolean.TRUE.equals(storageLocation.getStsEnabled())) {
			// This is the S3 bucket we own, so we need to auto-generate the base key.
			String baseKey = userInfo.getId() + "/" + UUID.randomUUID();
			storageLocation.setBaseKey(baseKey);
		}

		storageLocation.setUploadType(UploadType.S3);
	}
	
}
