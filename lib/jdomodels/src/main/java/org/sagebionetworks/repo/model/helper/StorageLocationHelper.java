package org.sagebionetworks.repo.model.helper;

import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StorageLocationHelper implements DaoObjectHelper<ExternalStorageLocationSetting> {
	
	StorageLocationDAO storageLocationDao;
	
	@Autowired
	public StorageLocationHelper(StorageLocationDAO storageLocationDao) {
		super();
		this.storageLocationDao = storageLocationDao;
	}


	@Override
	public ExternalStorageLocationSetting create(Consumer<ExternalStorageLocationSetting> consumer) {
		ExternalStorageLocationSetting sls = new ExternalStorageLocationSetting();
		sls.setDescription(UUID.randomUUID().toString());
		sls.setCreatedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		sls.setCreatedOn(new Date());
		consumer.accept(sls);
		long id = storageLocationDao.create(sls);
		return (ExternalStorageLocationSetting) storageLocationDao.get(id);
	}


	@Override
	public void truncateAll() {

		storageLocationDao.truncateAll();
	}

}
