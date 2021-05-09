package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_DATA_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_DESCRIPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_UPLOAD_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STORAGE_LOCATION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStorageLocation;
import org.sagebionetworks.repo.model.file.S3UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class DBOStorageLocationDAOImpl implements StorageLocationDAO, InitializingBean {

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;

	private static final int STORAGE_LOCATIONS_LIST_LIMIT = 100;

	private static final String STORAGE_LOCATION_IDS_PARAM = "udl_ids";

	private static final String SELECT_STORAGE_LOCATIONS_BY_IDS = "SELECT " + COL_STORAGE_LOCATION_ID + ", "
			+ COL_STORAGE_LOCATION_DESCRIPTION + ", " + COL_STORAGE_LOCATION_UPLOAD_TYPE + " FROM "
			+ TABLE_STORAGE_LOCATION + " WHERE " + COL_STORAGE_LOCATION_ID + " IN (:" + STORAGE_LOCATION_IDS_PARAM
			+ ")";

	private static final String SELECT_STORAGE_LOCATIONS_BY_OWNER = "SELECT * FROM " + TABLE_STORAGE_LOCATION
			+ " WHERE " + COL_STORAGE_LOCATION_CREATED_BY + " = ? ORDER BY " + COL_STORAGE_LOCATION_CREATED_ON
			+ " DESC LIMIT " + STORAGE_LOCATIONS_LIST_LIMIT;

	private static final String SELECT_ID_BY_CREATOR_AND_HASH = "SELECT " + COL_STORAGE_LOCATION_ID + " FROM "
			+ TABLE_STORAGE_LOCATION + " WHERE " + COL_STORAGE_LOCATION_CREATED_BY + " = ? AND "
			+ COL_STORAGE_LOCATION_DATA_HASH + " = ? ORDER BY " + COL_STORAGE_LOCATION_CREATED_ON + " DESC LIMIT 1";

	private static final RowMapper<DBOStorageLocation> ROW_MAPPER = new DBOStorageLocation().getTableMapping();

	public static UploadDestination getDefaultUploadDestination() {
		S3UploadDestination defaultUploadDestination = new S3UploadDestination();
		defaultUploadDestination.setStorageLocationId(DBOStorageLocationDAOImpl.DEFAULT_STORAGE_LOCATION_ID);
		defaultUploadDestination.setUploadType(UploadType.S3);
		return defaultUploadDestination;
	}

	private static StorageLocationSetting getDefaultStorageLocationSetting() {
		StorageLocationSetting storageLocationSetting = new S3StorageLocationSetting();
		storageLocationSetting.setStorageLocationId(DEFAULT_STORAGE_LOCATION_ID);
		storageLocationSetting.setUploadType(UploadType.S3);
		storageLocationSetting.setCreatedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		storageLocationSetting.setCreatedOn(new Date());
		return storageLocationSetting;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		SinglePrimaryKeySqlParameterSource params = new SinglePrimaryKeySqlParameterSource(
				DBOStorageLocationDAOImpl.DEFAULT_STORAGE_LOCATION_ID);
		if (!basicDao.getObjectByPrimaryKeyIfExists(DBOStorageLocation.class, params).isPresent()) {
			try {
				// make sure we skip the first couple of IDs
				idGenerator.generateNewId(IdType.STORAGE_LOCATION_ID);
				idGenerator.generateNewId(IdType.STORAGE_LOCATION_ID);
				S3StorageLocationSetting defaultStorageLocationSetting = (S3StorageLocationSetting) DBOStorageLocationDAOImpl
						.getDefaultStorageLocationSetting();
				defaultStorageLocationSetting.setCreatedOn(new Date());
				defaultStorageLocationSetting.setCreatedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
				defaultStorageLocationSetting
						.setStorageLocationId(DBOStorageLocationDAOImpl.DEFAULT_STORAGE_LOCATION_ID);
				create(defaultStorageLocationSetting);
			} catch (DuplicateKeyException e2) {
				// someone else got there first
			} catch (IllegalArgumentException e2) {
				if (!(e2.getCause() instanceof DuplicateKeyException)) {
					throw e2;
				}
			}
		}
	}

	@WriteTransaction
	@Override
	public Long create(StorageLocationSetting dto) {
		DBOStorageLocation dbo = StorageLocationUtils.convertDTOtoDBO(dto);

		Optional<Long> existingLocationId = findByCreatorAndHash(dbo.getCreatedBy(), dbo.getDataHash());

		if (existingLocationId.isPresent()) {
			return existingLocationId.get();
		}

		if (dbo.getId() == null) {
			dbo.setId(idGenerator.generateNewId(IdType.STORAGE_LOCATION_ID));
		}
		if (dbo.getEtag() == null) {
			dbo.setEtag(UUID.randomUUID().toString());
		}

		// PFLM-5985
		if (dbo.getUploadType() == null) {
			dbo.setUploadType(UploadType.NONE);
		}

		dbo = basicDao.createNew(dbo);
		return dbo.getId();
	}

	@WriteTransaction
	@Override
	public void delete(Long id) {
		basicDao.deleteObjectByPrimaryKey(DBOStorageLocation.class, new SinglePrimaryKeySqlParameterSource(id));
	}

	@Override
	public StorageLocationSetting get(Long storageLocationId) throws DatastoreException, NotFoundException {
		if (storageLocationId == null || DEFAULT_STORAGE_LOCATION_ID.equals(storageLocationId)) {
			return getDefaultStorageLocationSetting();
		}

		DBOStorageLocation dbo = basicDao.getObjectByPrimaryKey(DBOStorageLocation.class,
				new SinglePrimaryKeySqlParameterSource(storageLocationId));

		return StorageLocationUtils.convertDBOtoDTO(dbo);
	}

	@Override
	public List<StorageLocationSetting> getByOwner(Long id) throws DatastoreException, NotFoundException {
		List<DBOStorageLocation> dboStorageLocations = jdbcTemplate.query(SELECT_STORAGE_LOCATIONS_BY_OWNER, ROW_MAPPER,
				id);
		return dboStorageLocations.stream().map(StorageLocationUtils::convertDBOtoDTO).collect(Collectors.toList());
	}

	@Override
	public List<UploadDestinationLocation> getUploadDestinationLocations(List<Long> storageLocationIds)
			throws DatastoreException, NotFoundException {
		return namedParameterJdbcTemplate.query(SELECT_STORAGE_LOCATIONS_BY_IDS,
				Collections.singletonMap(STORAGE_LOCATION_IDS_PARAM, storageLocationIds),
				new RowMapper<UploadDestinationLocation>() {
					@Override
					public UploadDestinationLocation mapRow(ResultSet rs, int rowNum) throws SQLException {
						UploadDestinationLocation location = new UploadDestinationLocation();
						location.setStorageLocationId(rs.getLong(COL_STORAGE_LOCATION_ID));
						location.setDescription(rs.getString(COL_STORAGE_LOCATION_DESCRIPTION));
						location.setUploadType(UploadType.valueOf(rs.getString(COL_STORAGE_LOCATION_UPLOAD_TYPE)));
						return location;
					}
				});
	}

	private Optional<Long> findByCreatorAndHash(Long creatorId, String hash) throws DatastoreException {
		try {
			Long id = jdbcTemplate.queryForObject(SELECT_ID_BY_CREATOR_AND_HASH, Long.class, creatorId, hash);
			return Optional.of(id);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}
}
