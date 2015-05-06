package org.sagebionetworks.repo.manager.migration;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_STORAGE_LOCATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STORAGE_LOCATION;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.dao.DBOStorageLocationDAOImpl;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle.MetadataType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStorageLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.amazonaws.util.StringUtils;
import com.google.common.collect.Lists;

public class ExternalLocationIdSetterListener implements MigrationTypeListener {

	private static final String GET_ALL_STORAGE_LOCATIONS_SQL = "SELECT * FROM " + TABLE_STORAGE_LOCATION + " WHERE "
			+ COL_STORAGE_LOCATION_ID + " != " + DBOStorageLocationDAOImpl.DEFAULT_STORAGE_LOCATION_ID;
	private static final String UPDATE_STORAGE_LOCATION_ID_SQL = "UPDATE " + TABLE_FILES + " SET " + COL_FILES_STORAGE_LOCATION_ID
			+ " = ? WHERE " + COL_FILES_ID + " = ?";

	@Autowired
	private DBOBasicDao dboBasicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static class Location {
		String url;
		Long storageLocationId;

		public Location(String url, Long storageLocationId) {
			this.url = url;
			this.storageLocationId = storageLocationId;
		}
	}

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(MigrationType type, List<D> delta) {
		if (type != MigrationType.FILE_HANDLE) {
			return;
		}

		// get all storage locations (they are migrated before the file handles)
		List<DBOStorageLocation> storageLocations = jdbcTemplate.query(GET_ALL_STORAGE_LOCATIONS_SQL,
				new DBOStorageLocation().getTableMapping());
		List<Location> locations = Lists.newArrayList();
		for (DBOStorageLocation storageLocation : storageLocations) {
			if (storageLocation.getUploadType() != UploadType.SFTP) {
				continue;
			}
			if (!(storageLocation.getData() instanceof ExternalStorageLocationSetting)) {
				continue;
			}
			ExternalStorageLocationSetting externalStorageLocationSetting = (ExternalStorageLocationSetting) storageLocation.getData();
			if (StringUtils.isNullOrEmpty(externalStorageLocationSetting.getUrl())) {
				continue;
			}
			locations.add(new Location(externalStorageLocationSetting.getUrl(), storageLocation.getId()));
		}

		for (D d : delta) {
			DBOFileHandle fileHandle = (DBOFileHandle) d;
			if (fileHandle.getMetadataTypeEnum() == MetadataType.EXTERNAL && fileHandle.getStorageLocationId() == null) {
				// see if we can find the storage location id
				String url = fileHandle.getKey();
				if (url.startsWith("sftp://")) {
					for (Location location : locations) {
						if (url.startsWith(location.url)) {
							jdbcTemplate.update(UPDATE_STORAGE_LOCATION_ID_SQL, location.storageLocationId, fileHandle.getId());
						}
					}
				}
			}
		}
	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// nothing here
	}
}
