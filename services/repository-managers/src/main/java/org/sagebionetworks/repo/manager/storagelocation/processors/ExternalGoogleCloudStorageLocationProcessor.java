package org.sagebionetworks.repo.manager.storagelocation.processors;

import org.sagebionetworks.repo.manager.storagelocation.StorageLocationProcessor;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.springframework.stereotype.Service;

@Service
public class ExternalGoogleCloudStorageLocationProcessor implements StorageLocationProcessor<ExternalGoogleCloudStorageLocationSetting> {

	@Override
	public boolean supports(Class<? extends StorageLocationSetting> storageLocationClass) {
		return ExternalGoogleCloudStorageLocationSetting.class.isAssignableFrom(storageLocationClass);
	}

	@Override
	public void beforeCreate(UserInfo userInfo, ExternalGoogleCloudStorageLocationSetting storageLocation) {
		storageLocation.setUploadType(UploadType.GOOGLECLOUDSTORAGE);
	}

}
