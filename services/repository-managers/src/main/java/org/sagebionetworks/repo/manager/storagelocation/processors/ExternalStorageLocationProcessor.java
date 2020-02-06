package org.sagebionetworks.repo.manager.storagelocation.processors;

import org.sagebionetworks.repo.manager.storagelocation.StorageLocationProcessor;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class ExternalStorageLocationProcessor implements StorageLocationProcessor<ExternalStorageLocationSetting> {

	@Override
	public boolean supports(Class<? extends StorageLocationSetting> storageLocationClass) {
		return ExternalStorageLocationSetting.class.isAssignableFrom(storageLocationClass);
	}
	
	@Override
	public void beforeCreate(UserInfo userInfo, ExternalStorageLocationSetting storageLocation) {
		ValidateArgument.required(storageLocation.getUrl(), "The url");
		ValidateArgument.validExternalUrl(storageLocation.getUrl());
	}

}
