package org.sagebionetworks.repo.manager.storagelocation.processors;

import org.sagebionetworks.repo.manager.storagelocation.StorageLocationProcessor;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.StorageLocationUtils;
import org.sagebionetworks.repo.model.project.ExternalObjectStorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class ExternalObjectStorageLocationProcessor implements StorageLocationProcessor<ExternalObjectStorageLocationSetting> {

	@Override
	public boolean supports(Class<? extends StorageLocationSetting> storageLocationClass) {
		return ExternalObjectStorageLocationSetting.class.isAssignableFrom(storageLocationClass);
	}
	
	@Override
	public void beforeCreate(UserInfo userInfo, ExternalObjectStorageLocationSetting storageLocation) {
		// strip leading and trailing slashes and whitespace from the endpointUrl and bucket
		String strippedEndpoint = StorageLocationUtils.sanitizeEndpointUrl(storageLocation.getEndpointUrl());
		
		// validate url
		ValidateArgument.validExternalUrl(strippedEndpoint);

		// passed validation, set endpoint as the stripped version
		storageLocation.setEndpointUrl(strippedEndpoint);
	}

}
