package org.sagebionetworks.repo.manager.storagelocation.processors;

import org.sagebionetworks.repo.manager.storagelocation.StorageLocationProcessor;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.StorageLocationUtils;
import org.sagebionetworks.repo.model.project.BaseKeyStorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
// We run this storage location before any other to sanitize the base key
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BaseKeyStorageLocationProcessor implements StorageLocationProcessor<BaseKeyStorageLocationSetting> {

	@Override
	public boolean supports(Class<? extends StorageLocationSetting> storageLocationClass) {
		return BaseKeyStorageLocationSetting.class.isAssignableFrom(storageLocationClass);
	}

	@Override
	public void beforeCreate(UserInfo userInfo, BaseKeyStorageLocationSetting storageLocation) {
		
		String sanitizedBaseKey = StorageLocationUtils.sanitizeBaseKey(storageLocation.getBaseKey());
		
		storageLocation.setBaseKey(sanitizedBaseKey);
	}

}
