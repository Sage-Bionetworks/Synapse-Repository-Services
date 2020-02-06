package org.sagebionetworks.repo.manager.storagelocation.processors;

import org.sagebionetworks.repo.manager.storagelocation.StorageLocationProcessor;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.springframework.stereotype.Service;

@Service
public class ExternalS3StorageLocationProcessor implements StorageLocationProcessor<ExternalS3StorageLocationSetting> {

	@Override
	public boolean supports(Class<? extends StorageLocationSetting> storageLocationClass) {
		return ExternalS3StorageLocationSetting.class.isAssignableFrom(storageLocationClass);
	}

	@Override
	public void beforeCreate(UserInfo userInfo, ExternalS3StorageLocationSetting storageLocation) {
		storageLocation.setUploadType(UploadType.S3);
	}

}
