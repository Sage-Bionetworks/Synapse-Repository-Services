package org.sagebionetworks.repo.manager.migration;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UploadDestinationLocationDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBOProjectSetting;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.project.ExternalUploadDestinationLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalUploadDestinationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationSetting;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

public class ProjectSettingMigrationListener implements MigrationTypeListener {

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private UploadDestinationLocationDAO uploadDestinationLocationDAO;

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(MigrationType type, List<D> delta) {
		if (type != MigrationType.PROJECT_SETTINGS) {
			return;
		}

		try {
			for (D d : delta) {
				if (d instanceof DBOProjectSetting) {
					migrateProjectSetting((DBOProjectSetting) d);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private void migrateProjectSetting(DBOProjectSetting projectSetting) throws Exception {
		List<UploadDestinationLocationSetting> allSettings = uploadDestinationLocationDAO.getAllUploadDestinationLocationSettings();
		if (projectSetting.getData() instanceof UploadDestinationListSetting) {
			UploadDestinationListSetting list = (UploadDestinationListSetting) projectSetting.getData();
			if (list.getLocations() == null) {
				if (list.getDestinations() != null) {
					List<Long> uploadIds = Lists.newArrayList();
					for (UploadDestinationSetting destination : list.getDestinations()) {
						if (destination instanceof ExternalUploadDestinationSetting) {
							ExternalUploadDestinationSetting externalDestination = (ExternalUploadDestinationSetting) destination;

							// dedup the immutable location settings here
							Long uploadId = findSetting(allSettings, externalDestination.getSupportsSubfolders(),
									externalDestination.getUploadType(), externalDestination.getBanner(), externalDestination.getUrl());

							if (uploadId == null) {
								ExternalUploadDestinationLocationSetting locationSetting = new ExternalUploadDestinationLocationSetting();
								locationSetting.setSupportsSubfolders(externalDestination.getSupportsSubfolders());
								locationSetting.setUploadType(externalDestination.getUploadType());
								locationSetting.setBanner(externalDestination.getBanner());
								locationSetting.setUrl(externalDestination.getUrl());
								locationSetting.setDescription("Upload to " + externalDestination.getUrl());
								locationSetting.setCreatedOn(new Date());
								locationSetting.setCreatedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
								uploadId = uploadDestinationLocationDAO.create(locationSetting);
								locationSetting.setUploadId(uploadId);
								allSettings.add(locationSetting);
							}
							uploadIds.add(uploadId);
						} else {
							throw new RuntimeException("Migration cannot handle type " + destination.getClass().getName());
						}
					}

					// replace destinations with locations
					list.setLocations(uploadIds);
					list.setDestinations(null);
					projectSetting.setData(list);

					basicDao.update(projectSetting);
				}
			}
		}
	}

	private Long findSetting(List<UploadDestinationLocationSetting> allSettings, Boolean supportsSubfolders, UploadType uploadType,
			String banner, String url) {
		for (UploadDestinationLocationSetting setting : allSettings) {
			if (!(setting instanceof ExternalUploadDestinationLocationSetting)) {
				// this only works for this one type
				continue;
			}
			if (!ObjectUtils.equals(((ExternalUploadDestinationLocationSetting) setting).getSupportsSubfolders(), supportsSubfolders)) {
				continue;
			}
			if (!ObjectUtils.equals(((ExternalUploadDestinationLocationSetting) setting).getUploadType(), uploadType)) {
				continue;
			}
			if (!ObjectUtils.equals(((ExternalUploadDestinationLocationSetting) setting).getBanner(), banner)) {
				continue;
			}
			if (!ObjectUtils.equals(((ExternalUploadDestinationLocationSetting) setting).getUrl(), url)) {
				continue;
			}
			return setting.getUploadId();
		}
		return null;
	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// do nothing here
	}
}
