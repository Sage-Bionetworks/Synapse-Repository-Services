package org.sagebionetworks.migration.worker;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.DBOStorageLocationDAOImpl;
import org.sagebionetworks.repo.model.migration.CleanupStorageLocationsRequest;
import org.sagebionetworks.repo.model.migration.CleanupStorageLocationsResponse;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.util.TemporaryCode;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

@TemporaryCode(author = "marco.marasca@sagebase.org")
public class StorageLocationsCleanup {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private StorageLocationDAO storageLocationDao;

	@Autowired
	private ProjectSettingsDAO projectSettingsDao;

	private static Logger log = LogManager.getLogger(StorageLocationsCleanup.class);

	private static final String TABLE_PROJECT_SETTING_LOCATIONS = "PROJECT_SETTING_LOCATIONS";

	private static final int BATCH_UPDATE_SIZE = 1000;

	private static final int FILE_HANDLE_UPDATE_BATCH_SIZE = 100000;

	public CleanupStorageLocationsResponse cleanupStorageLocations(UserInfo userInfo,
			CleanupStorageLocationsRequest request) throws DatastoreException, UnauthorizedException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.requirement(request.getUsers() != null && !request.getUsers().isEmpty(),
				"The requets.users property cannot be null or empty");

		dropProjectSettingLocationsTable();

		createProjectSettingLocationTable();

		dumpProjectSettingLocations();

		Long droppedLocations = 0L;
		Long updatedFiles = 0L;
		Long deletedProjectSettings = 0L;
		Long duplicateLocations = 0L;

		droppedLocations += removeUnreferencedStorageLocations();

		for (Long userId : request.getUsers()) {
			updatedFiles += updateFileHandleReferencesForUsers(userId);
			deletedProjectSettings += dropProjectSettingsForUser(userId);
		}

		droppedLocations += removeUnreferencedStorageLocations();
		duplicateLocations += updateDuplicates();

		CleanupStorageLocationsResponse response = new CleanupStorageLocationsResponse();

		response.setUpdatedFilesCount(updatedFiles);
		response.setDeletedProjectSettingsCount(deletedProjectSettings);
		response.setDuplicateLocationsCount(duplicateLocations);
		response.setDroppedLocationsCount(droppedLocations);

		return response;
	}

	private int updateDuplicates() {
		log.info("Updating duplicate locations...");
		int duplicateLocations = 0;
		for (Long storageLocationId : storageLocationDao.findAllWithDuplicates()) {
			Set<Long> duplicates = storageLocationDao.findDuplicates(storageLocationId);
			updateLocationsHash(storageLocationId, duplicates);
			duplicateLocations += duplicates.size();
		}
		log.info("Updating duplicate locations...DONE ({} updated)", duplicateLocations);
		return duplicateLocations;
	}

	private void updateLocationsHash(Long masterLocationId, Set<Long> duplicates) {

		log.info("Updating data hash for {} storage locations from master location {}...", duplicates.size(),
				masterLocationId);

		String dataHash = jdbcTemplate.queryForObject("SELECT DATA_HASH FROM STORAGE_LOCATION WHERE ID = ?",
				String.class, masterLocationId);

		List<Pair<Long, String>> hashBatch = new ArrayList<>();

		int index = 1;

		for (Long duplicateId : duplicates) {
			hashBatch.add(ImmutablePair.of(duplicateId, dataHash + "_duplicate_of_" + masterLocationId + "_" + index));
			if (hashBatch.size() >= BATCH_UPDATE_SIZE) {
				updateLocationHashBatch(hashBatch);
				hashBatch.clear();
			}
			index++;
		}

		if (!hashBatch.isEmpty()) {
			updateLocationHashBatch(hashBatch);
			hashBatch.clear();
		}

		log.info("Updating data hash for {} storage locations from master location {}...DONE", duplicates.size(),
				masterLocationId);

	}

	private void updateLocationHashBatch(List<Pair<Long, String>> hashBatch) {
		jdbcTemplate.batchUpdate("UPDATE STORAGE_LOCATION SET DATA_HASH = ?, ETAG = UUID() WHERE ID = ?",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						Pair<Long, String> idHash = hashBatch.get(i);
						ps.setString(1, idHash.getRight());
						ps.setLong(2, idHash.getLeft());
					}

					@Override
					public int getBatchSize() {
						return hashBatch.size();
					}
				});
	}

	private int updateFileHandleReferencesForUsers(Long userId) {
		log.info("Updating file handle references for user {}...", userId);

		int currentBatch = 0;
		int affectedRows = 0;

		do {
			currentBatch = jdbcTemplate.update(
					"UPDATE FILES SET STORAGE_LOCATION_ID = NULL, ETAG = UUID() WHERE CREATED_BY = ? AND STORAGE_LOCATION_ID IS NOT NULL LIMIT ?",
					userId, FILE_HANDLE_UPDATE_BATCH_SIZE);
			affectedRows += currentBatch;
		} while (currentBatch >= FILE_HANDLE_UPDATE_BATCH_SIZE);

		log.info("Updating file handle references for user {}...DONE ({} updated)", userId, affectedRows);

		return affectedRows;
	}

	private int dropProjectSettingsForUser(Long userId) {
		log.info("Dropping project settings for user {}...", userId);

		int affectedRows = jdbcTemplate.update(
				"WITH REFERENCED_PS AS (SELECT PROJECT_SETTING_ID AS ID FROM PROJECT_SETTING_LOCATIONS AS PSL JOIN STORAGE_LOCATION AS SL ON PSL.STORAGE_LOCATION_ID = SL.ID WHERE SL.CREATED_BY = ?) "
						+ "DELETE FROM PROJECT_SETTING WHERE ID IN (SELECT ID FROM REFERENCED_PS)",
				userId);
		log.info("Dropping project settings for user {}...DONE ({} removed)", userId, affectedRows);

		return affectedRows;
	}

	private int removeUnreferencedStorageLocations() {
		log.info("Removing unreferenced storage locations...");

		int affectedRows = jdbcTemplate.update("WITH REFERENCED_SL AS ( "
				+ "SELECT DISTINCT SL.ID FROM PROJECT_SETTING_LOCATIONS AS PSL JOIN STORAGE_LOCATION AS SL ON PSL.STORAGE_LOCATION_ID = SL.ID "
				+ "UNION SELECT DISTINCT SL.ID FROM FILES AS F JOIN STORAGE_LOCATION AS SL ON F.STORAGE_LOCATION_ID = SL.ID) "
				+ "DELETE FROM STORAGE_LOCATION WHERE ID NOT IN (SELECT ID FROM REFERENCED_SL) AND ID <> " + DBOStorageLocationDAOImpl.DEFAULT_STORAGE_LOCATION_ID);

		log.info("Removing unreferenced storage locations...DONE ({} removed)", affectedRows);

		return affectedRows;
	}

	private void dropProjectSettingLocationsTable() {
		log.info("Dropping table {}", TABLE_PROJECT_SETTING_LOCATIONS);

		jdbcTemplate.update("DROP TABLE IF EXISTS `" + TABLE_PROJECT_SETTING_LOCATIONS + "`");
	}

	private void createProjectSettingLocationTable() {
		log.info("Creating table " + TABLE_PROJECT_SETTING_LOCATIONS);
		jdbcTemplate.update("CREATE TABLE IF NOT EXISTS `" + TABLE_PROJECT_SETTING_LOCATIONS + "` ( "
				+ "`PROJECT_SETTING_ID` BIGINT(20) NOT NULL, " + "`STORAGE_LOCATION_ID` BIGINT(20) NOT NULL, "
				+ "CONSTRAINT `FK_PSL_PROJECT_SETTING` FOREIGN KEY (`PROJECT_SETTING_ID`) REFERENCES `PROJECT_SETTING` (`id`) ON DELETE CASCADE, "
				+ "CONSTRAINT `FK_PSL_STORAGE_LOCATION` FOREIGN KEY (`STORAGE_LOCATION_ID`) REFERENCES `STORAGE_LOCATION` (`id`) ON DELETE CASCADE)");
	}

	private void dumpProjectSettingLocations() {

		log.info("Dumping project setting locations to table {}", TABLE_PROJECT_SETTING_LOCATIONS);

		Iterator<ProjectSetting> settings = projectSettingsDao.getByType(ProjectSettingsType.upload);

		List<Pair<Long, Long>> batch = new ArrayList<>();

		while (settings.hasNext()) {
			UploadDestinationListSetting setting = (UploadDestinationListSetting) settings.next();

			setting.getLocations().forEach(locationId -> {

				batch.add(ImmutablePair.of(Long.valueOf(setting.getId()), locationId));
				if (batch.size() >= BATCH_UPDATE_SIZE) {
					dumpProjectSettingLocationBatch(batch);
					batch.clear();
				}

			});
		}

		if (!batch.isEmpty()) {
			dumpProjectSettingLocationBatch(batch);
			batch.clear();
		}

	}

	private void dumpProjectSettingLocationBatch(List<Pair<Long, Long>> batch) {
		log.info("Adding batch of {} items to table {}", batch.size(), TABLE_PROJECT_SETTING_LOCATIONS);

		jdbcTemplate.batchUpdate(
				"INSERT INTO " + TABLE_PROJECT_SETTING_LOCATIONS
						+ " (PROJECT_SETTING_ID, STORAGE_LOCATION_ID) VALUES (?, ?)",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						Pair<Long, Long> pair = batch.get(i);
						ps.setLong(1, pair.getLeft());
						ps.setLong(2, pair.getRight());
					}

					@Override
					public int getBatchSize() {
						return batch.size();
					}
				});
	}

}
